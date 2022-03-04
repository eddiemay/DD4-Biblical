package com.digitald4.biblical.server;

import com.digitald4.biblical.model.*;
import com.digitald4.biblical.tools.CalendarValidator;
import com.google.api.server.spi.config.*;
import com.google.common.collect.Iterables;
import org.joda.time.DateTime;

import javax.inject.Inject;

@Api(
    name = "calendarValidator",
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
public class CalendarValidatorService {

  private final CalendarValidator calendarValidator;

  @Inject
  CalendarValidatorService(CalendarValidator calendarValidator) {
    this.calendarValidator = calendarValidator;
  }

  @ApiMethod(httpMethod = ApiMethod.HttpMethod.GET, path = "validate")
  public CalendarValidationResults validate(
      DateTime startDate, @Named("type") @Nullable BiblicalCalendar.Type type, Iterable<CalendarRule> calRules) {
    BiblicalCalendar calendar =
        type == BiblicalCalendar.Type.Lunar ? new LunarCalendar(startDate) : new EnochCalendar(startDate);

    return calendarValidator.validate(calendar, Iterables.isEmpty(calRules) ? DayLengthCalendarRule.ALL : calRules);
  }
}
