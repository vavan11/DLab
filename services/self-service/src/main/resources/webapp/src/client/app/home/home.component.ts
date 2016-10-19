import { Component, OnInit, ViewChild } from '@angular/core';
import { Router } from '@angular/router';
import { Http, Response } from '@angular/http';
import { Observable }     from 'rxjs/Observable';

import { AuthenticationService } from './../security/authentication.service';
import { UserProfileService } from "../security/userProfile.service";
import { EnvironmentsService } from './home.service';
//import { ModalModule } from './../components/modal/index';

import { ResourcesList } from '../components/list-of-resources/resources.component';

@Component({
  moduleId: module.id,
  selector: 'sd-home',
  templateUrl: 'home.component.html',
  styleUrls: ['./home.component.css'],
  providers: [AuthenticationService]
})

export class HomeComponent implements OnInit {
  key: any;
  keyStatus: number;

  environments: any;
  collapse: boolean = false;

  @ViewChild('keyUploadModal') keyUploadModal;
  @ViewChild('preloaderModal') preloaderModal;

  constructor(
    private http: Http,
    private authenticationService: AuthenticationService,
    private router: Router,
    private userProfileService: UserProfileService,
    private environmentsService: EnvironmentsService
    ) {}

   logout() {
     this.authenticationService.logout().subscribe(
       data => data,
       err => console.log(err),
       () => this.router.navigate(['/login']));
   }


  ngOnInit() {
    this.checkKey()
    .subscribe(
      data => {
        this.key = data;
      },
      err => {
        this.keyStatus = err.status;
        this.keyUploadModal.open({isFooter: false});
      }
    );

    this.environmentsService.getEnvironmentsList().subscribe((list) => {
      this.environments = list['resources'];
    });
  }

  uploadingKey(event) {
    this.keyUploadModal.close();
    this.preloaderModal.open({isHeader: false, isFooter: false});
    setTimeout(function () {
      this.preloaderModal.close();
    }.bind(this), 10000);
  }

  checkKey() {
    return this.http.get(`/api/keyloader=?access_token=${this.userProfileService.getAuthToken()}`).map(( res:Response ) => res.json());
  }

  toggleResourceList() {
    console.log('toggle');
    this.collapse = !this.collapse;
  }












  // openModal(className: string) {


  //   [].forEach.call(document.querySelectorAll('.modal'), function(modal) {
  //     modal.setAttribute('open', '');
  //   });
  //   document.querySelector(className).setAttribute('open', 'true');

  // }

  // closeModal(className: string) {
  //   /*
  //   document.querySelector(className).setAttribute('open', '');
  //   */
  // }
}
