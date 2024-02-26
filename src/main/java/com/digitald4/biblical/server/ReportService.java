package com.digitald4.biblical.server;

import com.digitald4.biblical.report.CalendarPDF;
import com.digitald4.common.exception.DD4StorageException;
import com.digitald4.common.server.service.Empty;
import com.google.api.server.spi.ServiceException;
import com.google.api.server.spi.config.*;
import com.itextpdf.text.DocumentException;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Api(
    name = "reports",
    version = "v1",
    namespace = @ApiNamespace(
        ownerDomain = "iis.digitald4.com",
        ownerName = "iis.digitald4.com"
    )
)
public class ReportService {
  private final Provider<HttpServletResponse> responseProvider;
  private final CalendarPDF calendarPDF;

  @Inject
  public ReportService(Provider<HttpServletResponse> responseProvider, CalendarPDF calendarPDF) {
    this.responseProvider = responseProvider;
    this.calendarPDF = calendarPDF;
  }

  @ApiMethod(httpMethod = ApiMethod.HttpMethod.GET, path = "calendar/{filename}")
  public Empty getCalendar(@Named("filename") String filename) throws ServiceException {
    // Enoch-Calendar-{year}.pdf
    int year = Integer.parseInt(filename.substring("Enoch-Calendar-".length(), filename.length() - 4));
    try {
      byte[] bytes = calendarPDF.createPDF(year).toByteArray();
      HttpServletResponse response = responseProvider.get();
      response.setContentType("application/pdf");
      response.setContentLength(bytes.length);
      response.getOutputStream().write(bytes);
    } catch (DocumentException | IOException e) {
      throw new ServiceException(DD4StorageException.ErrorCode.INTERNAL_SERVER_ERROR.getErrorCode(), e);
    }
    return null;
  }
}
