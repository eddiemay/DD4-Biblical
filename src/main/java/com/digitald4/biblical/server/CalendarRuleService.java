package com.digitald4.biblical.server;

import com.digitald4.biblical.model.CalendarRule;
import com.digitald4.biblical.model.DayLengthCalendarRule;
import com.google.api.server.spi.config.*;
import com.google.common.collect.ImmutableList;

@Api(
    name = "calendarRules",
    version = "v1",
    namespace = @ApiNamespace(
        ownerDomain = "biblical.digitald4.com",
        ownerName = "biblical.digitald4.com"
    ),
    // [START_EXCLUDE]
    issuers = {
        @ApiIssuer(
            name = "firebase",
            issuer = "https://securetoken.google.com/biblical",
            jwksUri = "https://www.googleapis.com/robot/v1/metadata/x509/securetoken@system.gserviceaccount.com")
    }
    // [END_EXCLUDE]
)
public class CalendarRuleService {
  @ApiMethod(httpMethod = ApiMethod.HttpMethod.GET, path = "_")
  public ImmutableList<CalendarRule> list() {
    return DayLengthCalendarRule.ALL;
  }
}
