/*
 * Copyright (c) 2018, EPAM SYSTEMS INC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.epam.dlab.backendapi.schedulers;

import com.epam.dlab.auth.SystemUserInfoServiceImpl;
import com.epam.dlab.auth.UserInfo;
import com.epam.dlab.backendapi.domain.RequestId;
import com.epam.dlab.backendapi.service.ExploratoryServiceImpl;
import com.epam.dlab.backendapi.service.SchedulerJobsService;
import com.epam.dlab.model.scheduler.SchedulerJobData;
import com.fiestacabin.dropwizard.quartz.Scheduled;
import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.JobExecutionContext;

import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.List;


@Slf4j
@Scheduled(interval = 10)
public class StartExploratoryJob implements Job {

    @Inject
    private SchedulerJobsService schedulerJobsService;

    @Inject
    private ExploratoryServiceImpl exploratoryService;

    @Inject
    private SystemUserInfoServiceImpl systemUserService;

    @Override
    public void execute(JobExecutionContext jobExecutionContext){
		OffsetDateTime currentDateTime = OffsetDateTime.now();
		List<SchedulerJobData> jobsToStart = schedulerJobsService.getSchedulerJobsForExploratoryAction
				(currentDateTime, "start");
        if(!jobsToStart.isEmpty()){
            log.info("Scheduler start job is executing...");
			log.info("Current time rounded: {} , current date: {}, current day of week: {}",
					LocalTime.of(currentDateTime.toLocalTime().getHour(), currentDateTime.toLocalTime().getMinute()),
					currentDateTime.toLocalDate(),
					currentDateTime.getDayOfWeek());
            log.info("Scheduler jobs for starting: {}", jobsToStart.size());
            for(SchedulerJobData jobData : jobsToStart){
                log.info("Exploratory with name {} for user {} is starting...", jobData.getExploratoryName(), jobData.getUser());
                UserInfo userInfo = systemUserService.create(jobData.getUser());
                String uuid = exploratoryService.start(userInfo, jobData.getExploratoryName());
                RequestId.put(userInfo.getName(), uuid);
            }

        }

    }
}
