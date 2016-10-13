#!/usr/bin/python
from fabric.api import *
from fabric.contrib.files import exists
import argparse
import json
import random
import string
import crypt
import sys

parser = argparse.ArgumentParser()
parser.add_argument('--hostname', type=str, default='edge')
parser.add_argument('--keyfile', type=str, default='')
parser.add_argument('--additional_config', type=str, default='{"empty":"string"}')
args = parser.parse_args()


def id_generator(size=10, chars=string.digits + string.ascii_letters):
    return ''.join(random.choice(chars) for _ in range(size))


def cp_key():
    try:
        key_name=args.keyfile.split("/")
        sudo('mkdir /project_images; chown ubuntu:ubuntu /project_images')
        local('scp -r -q -i {0} {0} {1}:/project_images/{2}'.format(args.keyfile, env.host_string, key_name[-1]))
        sudo('chmod 600 /project_images/*.pem')
        sudo('mkdir -p /home/ubuntu/keys')
        sudo('chown -R ubuntu:ubuntu /home/ubuntu/keys')
        local('scp -r -q -i {0} {0} {1}:/home/ubuntu/keys/{2}'.format(args.keyfile, env.host_string, key_name[-1]))
        sudo('chmod 600 /home/ubuntu/keys/*.pem')
        return True
    except:
        return False


def ensure_nginx():
    try:
        if not exists('/tmp/nginx_ensured'):
            sudo('apt-get -y install nginx')
            sudo('service nginx restart')
            sudo('sysv-rc-conf nginx on')
            sudo('touch /tmp/nginx_ensured')
        return True
    except:
        return False


def configure_nginx(config):
    try:
        random_file_part = id_generator(size=20)
        if not exists("/etc/nginx/conf.d/nginx_proxy.conf"):
            sudo('rm -f /etc/nginx/conf.d/*')
            put(config['nginx_template_dir'] + 'nginx_proxy.conf', '/tmp/nginx_proxy.conf')
            sudo('\cp /tmp/nginx_proxy.conf /etc/nginx/conf.d/')
            sudo('mkdir -p /etc/nginx/locations')
            sudo('rm -f /etc/nginx/sites-enabled/default')
    except:
        return False

    try:
        if not exists("/etc/nginx/locations/proxy_location_jenkins.conf"):
            nginx_password = id_generator()
            template_file = config['nginx_template_dir'] + 'proxy_location_jenkins_template.conf'
            with open("/tmp/%s-tmpproxy_location_jenkins_template.conf" % random_file_part, 'w') as out:
                with open(template_file) as tpl:
                    for line in tpl:
                        out.write(line)
            put("/tmp/%s-tmpproxy_location_jenkins_template.conf" % random_file_part, '/tmp/proxy_location_jenkins.conf')
            sudo('\cp /tmp/proxy_location_jenkins.conf /etc/nginx/locations/')
            sudo("echo 'engineer:" + crypt.crypt(nginx_password, id_generator()) + "' > /etc/nginx/htpasswd")
            with open('jenkins_crids.txt', 'w+') as f:
                f.write("Jenkins credentials: engineer  / " + nginx_password)
    except:
        return False

    try:
        sudo('service nginx reload')
        return True
    except:
        return False


def place_notebook_automation_scripts():
    try:
        sudo('mkdir -p /usr/share/notebook_automation')
        sudo('chown -R ubuntu /usr/share/notebook_automation')

        local('scp -r -i %s /usr/share/notebook_automation/* %s:/usr/share/notebook_automation/'
              % (args.keyfile, env.host_string))

        sudo('chmod a+x /usr/share/notebook_automation/scripts/*')
        sudo('chmod +x /usr/share/notebook_automation/fabfile_notebook.py')
        sudo('ln -s /usr/share/notebook_automation/fabfile_notebook.py /usr/local/bin/provision_notebook_server.py')
        sudo('ln -s /usr/share/notebook_automation/scripts/create_cluster.py /usr/local/bin/create_cluster.py')
        sudo('ln -s /usr/share/notebook_automation/scripts/terminate_aws_resources.py '
             '/usr/local/bin/terminate_aws_resources.py')
        sudo('echo export PROVISION_CONFIG=/usr/share/notebook_automation/conf/conf.ini >> /etc/profile')
        return True
    except:
        return False


def ensure_jenkins():
    try:
        if not exists('/tmp/jenkins_ensured'):
            sudo('wget -q -O - https://pkg.jenkins.io/debian/jenkins-ci.org.key | apt-key add -')
            sudo('echo deb http://pkg.jenkins.io/debian-stable binary/ > /etc/apt/sources.list.d/jenkins.list')
            sudo('apt-get -y update')
            sudo('apt-get -y install jenkins')
            sudo('touch /tmp/jenkins_ensured')
        return True
    except:
        return False


def configure_jenkins():
    try:
        if not exists('/tmp/jenkins_configured'):
            sudo('echo \'JENKINS_ARGS="--prefix=/jenkins --httpPort=8070"\' >> /etc/default/jenkins')
            sudo('rm -rf /var/lib/jenkins/*')
            sudo('mkdir -p /var/lib/jenkins/jobs/')
            sudo('chown -R ubuntu:ubuntu /var/lib/jenkins/')
            put('/root/templates/jenkins_jobs/*', '/var/lib/jenkins/jobs/')
            #local('scp -r -q -i {} /root/templates/jenkins_jobs/* {}:/var/lib/jenkins/jobs/'.format(args.keyfile, env.host_string))
            sudo('chown -R jenkins:jenkins /var/lib/jenkins')
            sudo('/etc/init.d/jenkins stop; sleep 5')
            sudo('sysv-rc-conf jenkins on')
            sudo('service jenkins start')
            sudo('touch /tmp/jenkins_configured')
            sudo('echo "jenkins ALL = NOPASSWD:ALL" >> /etc/sudoers')
        return True
    except:
        return False


def configure_proxy_server(config):
    try:
        if not exists('/tmp/proxy_ensured'):
            with settings(warn_only=True):
                sudo('apt-get -y install squid')
            template_file = config['squid_template_file']
            proxy_port = config['proxy_port']
            proxy_subnet = config['proxy_subnet']
            with open("/tmp/tmpsquid.conf", 'w') as out:
                with open(template_file) as tpl:
                    for line in tpl:
                        out.write(line.replace('PROXY_SUBNET', proxy_subnet)
                                  .replace('PROXY_PORT', proxy_port))
            put('/tmp/tmpsquid.conf', '/tmp/squid.conf')
            sudo('\cp /tmp/squid.conf /etc/squid/squid.conf')
            sudo('service squid reload')
            sudo('sysv-rc-conf squid on')
            sudo('touch /tmp/proxy_ensured')
        return True
    except:
        return False


##############
# Run script #
##############
if __name__ == "__main__":
    print "Configure connections"
    try:
        env['connection_attempts'] = 100
        env.key_filename = [args.keyfile]
        env.host_string = 'ubuntu@' + args.hostname
        deeper_config = json.loads(args.additional_config)
    except:
        sys.exit(2)

    print "Installing nginx as frontend."
    if not ensure_nginx():
        sys.exit(1)

    print "Configuring nginx."
    if not configure_nginx(deeper_config):
        sys.exit(1)

    print "Installing proxy for notebooks."
    if not configure_proxy_server(deeper_config):
        sys.exit(1)

    print "Installing jenkins."
    if not ensure_jenkins():
        sys.exit(1)

    print "Configuring jenkins."
    if not configure_jenkins():
        sys.exit(1)

    print "Copying key"
    if not cp_key():
        sys.exit(1)

    sys.exit(0)
