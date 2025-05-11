package jp.ecuacion.tool.housekeepdb;

import jp.ecuacion.splib.batch.SplibBatchApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Defines {@code @SpringBootApplication}.
 */
@SpringBootApplication
public class BatchApplication extends SplibBatchApplication {

  /**
   * Provides main method.
   * 
   * @param args args
   */
  public static void main(String[] args) {
    SplibBatchApplication.main(BatchApplication.class, args);
  }
}
