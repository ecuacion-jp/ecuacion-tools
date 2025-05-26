/*
 * Copyright © 2012 ecuacion.jp (info@ecuacion.jp)
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
package jp.ecuacion.tool.housekeepfiles.config;

import jp.ecuacion.splib.batch.config.SplibAppParentBatchConfig;
import jp.ecuacion.tool.housekeepfiles.tasklet.HousekeepFilesTasklet;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Provides config.
 */
@Configuration
@ComponentScan(basePackages = "jp.ecuacion.splib.batch.config")
public class AppBatchConfig extends SplibAppParentBatchConfig {

  @Autowired
  private HousekeepFilesTasklet housekeepFilesTasklet;

  /* HousekeepFilesTasklet */

  @Bean(name = "housekeepFilesJob")
  Job housekeepFilesJob(JobRepository jobRepository,
      PlatformTransactionManager transactionManager) {

    return preparedJobBuilder("housekeepFilesJob", jobRepository)
        .start(housekeepFilesJobStep1(jobRepository, transactionManager)).build();
  }

  @Bean
  Step housekeepFilesJobStep1(JobRepository jobRepository,
      PlatformTransactionManager transactionManager) {

    return preparedStepBuilder("housekeepFilesJobStep1", jobRepository, transactionManager,
        housekeepFilesTasklet).build();
  }
}
