package com.digitald4.biblical.report;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.ImmutableListMultimap.toImmutableListMultimap;
import static com.itextpdf.text.FontFactory.getFont;
import static java.util.Comparator.comparing;
import static java.util.function.Function.*;

import com.digitald4.biblical.model.BiblicalEvent;
import com.digitald4.biblical.model.EventDate;
import com.digitald4.biblical.model.HolyDay;
import com.digitald4.biblical.store.BiblicalEventStore;
import com.digitald4.biblical.util.Constants;
import com.digitald4.common.model.Company;
import com.digitald4.common.report.PDFReport;
import com.digitald4.common.server.APIConnector;
import com.digitald4.common.storage.ChangeTracker;
import com.digitald4.common.storage.DAO;
import com.digitald4.common.storage.DAOApiImpl;
import com.digitald4.common.storage.DAOFileBasedImpl;
import com.digitald4.common.storage.Query;
import com.digitald4.common.storage.Query.Filter;
import com.digitald4.common.util.FormatText;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.FontFactory;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.BaseFont;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;

import java.awt.Desktop;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Provider;

import org.joda.time.DateTime;

public class CalendarPDF extends PDFReport {
  private static final BaseColor FEAST_COLOR = new BaseColor(132, 14, 206);
  private static final Font FEAST_FONT = getFont(FontFactory.HELVETICA, 12, Font.BOLD, FEAST_COLOR);
  private final BiblicalEventStore biblicalEventStore;

  @Inject
  public CalendarPDF(Provider<Company> companyProvider, BiblicalEventStore biblicalEventStore) {
    super(companyProvider);
    this.biblicalEventStore = biblicalEventStore;
  }

  @Override
  public String getTitle() {
    return "True Enoch Calendar";
  }

  public String getTitle(int year, int month) {
    return String.format("%s Year %d Month %d (Gregorian Year %d-%d)", getTitle(), year + 3959, month, year, year + 1);
  }

  public Rectangle getPageSize() {
    return PageSize.A4.rotate();
  }

  @Override
  public Paragraph getBody() {
    return null;
  }

  public ByteArrayOutputStream createPDF(int year) throws DocumentException {
    Document document = new Document(getPageSize(), 25, 25, 25, 25);
    document.addAuthor(getAuthor());
    document.addSubject(getSubject());
    document.addTitle(getTitle());
    ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    PdfWriter.getInstance(document, buffer);
    document.open();
    // document.resetHeader();
    //document.setHeader(getHeader());
    // document.setFooter(getFooter());
    ImmutableListMultimap<String, BiblicalEvent> events = biblicalEventStore
        .list(Query.forList(Filter.of("day", ">", 0))).getItems().stream()
        .sorted(comparing(BiblicalEvent::getMonth)
            .thenComparing(BiblicalEvent::getDay).thenComparing(BiblicalEvent::getYear))
        .collect(toImmutableListMultimap(e -> String.format("%d-%d", e.getMonth(), e.getDay()), identity()));
    document.newPage();
    document.add(getReportTitle());
    // document.add(getBody(year));
    DateTime yearStartDate = DateTime.parse(year + "-03-20T08:00:00Z");
    if (yearStartDate.getDayOfWeek() > 4) {
      yearStartDate = yearStartDate.plusDays(7 - yearStartDate.getDayOfWeek());
    } else {
      yearStartDate = yearStartDate.minusDays(yearStartDate.getDayOfWeek());
    }
    for (int month = 1; month <= 12; month++) {
      document.add(getMonth(yearStartDate, year, month, events));
      document.newPage();
    }

    document.add(getScriptureRefPage(events));
    document.close();
    return buffer;
  }

  public Paragraph getMonth(DateTime yearStart, int year, int month, ImmutableListMultimap<String, BiblicalEvent> events)
      throws DocumentException {
    Paragraph body = new Paragraph();
    body.setFont(getFont("arialuni.ttf", BaseFont.IDENTITY_H, true, 9));
    body.setAlignment(Element.ALIGN_CENTER);
    body.add(new Phrase(getTitle(year, month), getFont(FontFactory.HELVETICA, 20, Font.BOLD)));
    PdfPTable datatable = new PdfPTable(8);
    datatable.setWidthPercentage(100);
    datatable.setWidths(new int[]{4, 14, 14, 13, 14, 13, 14, 14});
    int startDay = (int) Math.floor((month - 1) * 30.334 + 1);
    int baseWeek = (int) Math.floor((month - 1) * 4.334 + 1);
    int currentDay = (baseWeek - 1) * 7 + 1;

    datatable.addCell(createHeaderCell("Week"));
    for (int d = 1; d < 7; d++) {
      datatable.addCell(createHeaderCell(String.format("Day %d", d)));
    }
    datatable.addCell(createSabbathHeaderCell());
    for (int w = 0; w < 5; w++) {
      int week = baseWeek + w;
      datatable.addCell(createRowHeader(week));
      for (int d = 0; d < 7; d++) {
        datatable.addCell(getDay(yearStart, month, currentDay - startDay + 1, events));
        currentDay++;
      }
    }
    body.add(datatable);
    return body;
  }

  public PdfPCell getDay(DateTime yearStart, int month, int day, ImmutableListMultimap<String, BiblicalEvent> events) {
    if (day < 1 || day > 31 || day == 31 && month % 3 != 0) {
      return createCell("");
    }

    Paragraph body = new Paragraph();
    EventDate eventDate = new EventDate(month, day);

    String dayNumber = day == 31 ? "" : String.format("%d       ", day);
    body.add(new Phrase(
        dayNumber, eventDate.getDayOfYear() % 7 == 0 ? FEAST_FONT : getFont(FontFactory.HELVETICA, 10, Font.BOLD)));
    body.add(new Phrase(
        FormatText.formatDate(yearStart.plusDays(eventDate.getDayOfYear() - 1), new SimpleDateFormat("MMM dd\n"))));

    HolyDay.HOLY_DAYS.stream().filter(hd -> hd.getMatcher().apply(eventDate)).findFirst()
        .ifPresent(holyDay -> body.add(new Phrase(holyDay.getTitle() + "\n", FEAST_FONT)));

    events.get(String.format("%d-%d", month, day)).stream().limit(7).forEach(event -> body.add(
        new Phrase(replaceHebrew(event.getTitle()) + "\n", getFont("arialuni.ttf", BaseFont.IDENTITY_H, true, 9))));

    PdfPCell cell = new PdfPCell(body);
    int grayScale = eventDate.getWeekOfYear() % 2 == 1 ? 252 : 242;
    cell.setBackgroundColor(new BaseColor(grayScale, grayScale, grayScale));
    return cell;
  }

  public Paragraph getScriptureRefPage(ImmutableListMultimap<String, BiblicalEvent> eventMap) {
    Paragraph body = new Paragraph();
    body.setAlignment(Element.ALIGN_CENTER);
    body.add(new Phrase("Reference Scriptures", getFont(FontFactory.HELVETICA, 16, Font.BOLD)));

    PdfPTable datatable = new PdfPTable(3);
    datatable.setWidthPercentage(100);

    Paragraph holyBody = new Paragraph();
    holyBody.setFont(getFont("arialuni.ttf", BaseFont.IDENTITY_H, true, 9));
    HolyDay.HOLY_DAYS.stream().filter(hd -> hd.getStart() == null || hd.getStart().getMonth() < 13).forEach(hd -> {
      String startDate = hd.getStart() == null ? "" : hd.getStart().getMonth() + "/" + hd.getStart().getDay();
      String endDate = hd.getEnd() == null ? "" : "-" + hd.getEnd().getMonth() + "/" + hd.getEnd().getDay();
      holyBody.add(new Phrase(
          String.format("%s%s - %s - %s %s\n", startDate, endDate, replaceHebrew(hd.getTitle()), hd.getReferences(), hd.getSummary()),
          getFont("arialuni.ttf", BaseFont.IDENTITY_H, true, 9)));
    });
    datatable.addCell(new PdfPCell(holyBody));

    ImmutableList<BiblicalEvent> events =
        eventMap.entries().stream().map(Map.Entry::getValue).collect(toImmutableList());

    int batchSize = 18;
    int batches = (int) Math.ceil(events.size() / (batchSize * 1.0));
    for (int batch = 0; batch < batches; batch++) {
      Paragraph cellBody = new Paragraph();
      cellBody.setFont(getFont("arialuni.ttf", BaseFont.IDENTITY_H, true, 9));

      events.stream().skip((long) batch * batchSize).limit(batchSize).forEach(e -> cellBody.add(new Phrase(
          String.format("%d/%d - %s - %s\n", e.getMonth(), e.getDay(), replaceHebrew(e.getTitle()), e.referenceScriptures()),
          getFont("arialuni.ttf", BaseFont.IDENTITY_H, true, 9))));
      datatable.addCell(new PdfPCell(cellBody));
    }

    body.add(datatable);
    return body;
  }

  private static String replaceHebrew(String text) {
    return text.replaceAll("יהושע", "Messiah").replaceAll("יהוה", "Yah");
  }

  private static PdfPCell createHeaderCell(String text) {
    PdfPCell cell = new PdfPCell(new Phrase(text, getFont(FontFactory.HELVETICA, 10, Font.BOLD)));
    cell.setHorizontalAlignment(Element.ALIGN_CENTER);
    cell.setBackgroundColor(new BaseColor(204, 204, 204));
    return cell;
  }

  private static PdfPCell createSabbathHeaderCell() {
    PdfPCell cell = new PdfPCell(new Phrase("Shabbat", getFont(FontFactory.HELVETICA, 10, Font.BOLD, FEAST_COLOR)));
    cell.setHorizontalAlignment(Element.ALIGN_CENTER);
    cell.setBackgroundColor(new BaseColor(204, 204, 204));
    return cell;
  }

  private static PdfPCell createRowHeader(int week) {
    PdfPCell cell = new PdfPCell(new Phrase(String.format("\n\n\n%d\n\n\n", week), getFont(FontFactory.HELVETICA, 9)));
    cell.setHorizontalAlignment(Element.ALIGN_CENTER);
    cell.setBackgroundColor(week % 2 == 1 ? new BaseColor(232, 232, 232) : new BaseColor(216, 216, 216));
    return cell;
  }

  public static void main(String[] args) throws Exception {
    ChangeTracker changeTracker = new ChangeTracker( null, null, null, null);
    DAO fileDAO = new DAOFileBasedImpl(changeTracker, "data/biblical-events.jsonl").loadFromFile();

    /* APIConnector apiConnector = new APIConnector(Constants.API_URL, Constants.API_VERSION, 100).loadIdToken();
    DAO apiDAO = new DAOApiImpl(apiConnector);
    fileDAO.create(new BiblicalEventStore(() -> apiDAO, null).list(Query.forList(Filter.of("Day", ">", 0))).getItems());
    fileDAO.saveToFile(); */

    Company company = new Company().setName("Mackabee Ministries");
    BiblicalEventStore biblicalEventStore = new BiblicalEventStore(() -> fileDAO, null);
    byte[] bytes = new CalendarPDF(() -> company, biblicalEventStore).createPDF(2024).toByteArray();

    BufferedOutputStream output = new BufferedOutputStream(Files.newOutputStream(Paths.get("data/Calendar.pdf")));
    System.out.println(bytes.length);
    output.write(bytes);
    output.close();
    File file = new File("data/Calendar.pdf");
    Desktop.getDesktop().open(file);
    System.exit(0);
  }
}
