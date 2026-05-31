package jp.ecuacion.tool.housekeepdb.config;

import java.util.Objects;
import jp.ecuacion.splib.batch.config.SplibAppParentBatchConfig;
import jp.ecuacion.splib.batch.exceptionhandler.SplibExceptionHandler;
import jp.ecuacion.splib.batch.listener.SplibJobExecutionListener;
import jp.ecuacion.splib.batch.listener.SplibStepExecutionListener;
import jp.ecuacion.tool.housekeepdb.tasklet.HousekeepDbTasklet;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Provides batch config.
 */
@Configuration
@ComponentScan(basePackages = "jp.ecuacion.splib.batch.config")
@SuppressWarnings("NullAway.Init")
public class AppBatchConfig extends SplibAppParentBatchConfig {

  @Autowired
  private HousekeepDbTasklet housekeepDbTasklet;

  /**
   * Constructs a new instance.
   *
   * @param jobExecutionListener jobExecutionListener
   * @param stepExecutionListener stepExecutionListener
   * @param exceptionHandler exceptionHandler
   */
  public AppBatchConfig(SplibJobExecutionListener jobExecutionListener,
      SplibStepExecutionListener stepExecutionListener,
      SplibExceptionHandler exceptionHandler) {
    super(jobExecutionListener, stepExecutionListener, exceptionHandler);
  }

  @Bean(name = "housekeepDbJob")
  Job housekeepDbJob(JobRepository jobRepository,
      PlatformTransactionManager transactionManager) {

    return preparedJobBuilder("housekeepDbJob", jobRepository)
        .start(housekeepDbJobStep1(jobRepository, transactionManager)).build();
  }

  @Bean
  Step housekeepDbJobStep1(JobRepository jobRepository,
      PlatformTransactionManager transactionManager) {

    return Objects.requireNonNull(preparedStepBuilder("housekeepDbJobStep1", jobRepository,
        transactionManager, housekeepDbTasklet)).build();
  }
}
