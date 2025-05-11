package jp.ecuacion.tool.housekeepdb.config;

import jp.ecuacion.splib.batch.config.SplibAppParentBatchConfig;
import jp.ecuacion.tool.housekeepdb.tasklet.HousekeepDbTasklet;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@ComponentScan(basePackages = "jp.ecuacion.splib.batch.config")
public class AppBatchConfig extends SplibAppParentBatchConfig {

  @Autowired
  private HousekeepDbTasklet housekeepDbTasklet;

  @Bean(name = "housekeepDbJob")
  Job housekeepDbJob(JobRepository jobRepository,
      PlatformTransactionManager transactionManager) {

    return preparedJobBuilder("housekeepDbJob", jobRepository)
        .start(housekeepDbJobStep1(jobRepository, transactionManager)).build();
  }

  @Bean
  Step housekeepDbJobStep1(JobRepository jobRepository,
      PlatformTransactionManager transactionManager) {

    return preparedStepBuilder("housekeepDbJobStep1", jobRepository, transactionManager,
        housekeepDbTasklet).build();
  }
}
