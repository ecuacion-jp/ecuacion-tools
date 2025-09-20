package jp.ecuacion.util.commandapi.web.controller;

import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Returns OK always as long as it's alive.
 */
@RestController
public class AliveCheckController {

  /**
   * Returns OK always as long as it's alive.
   * 
   * @return returnCode map
   * @throws Exception Exception
   */
  @GetMapping("api/public/aliveCheck")
  public Map<String, String> aliveCheck() throws Exception {
    return Map.of("returnCode", "0");
  }
}
