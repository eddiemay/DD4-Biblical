package com.digitald4.biblical.store;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.joining;

import com.digitald4.biblical.model.Interlinear;
import com.digitald4.biblical.model.Scripture.InterlinearScripture;
import com.digitald4.biblical.util.InterlinearFetcher;
import com.digitald4.biblical.util.ScriptureReferenceProcessor;
import com.digitald4.biblical.util.ScriptureReferenceProcessor.VerseRange;
import com.digitald4.common.storage.DAO;
import com.digitald4.common.storage.GenericStore;
import com.digitald4.common.storage.Query;
import com.digitald4.common.storage.Query.Filter;
import com.digitald4.common.storage.Query.OrderBy;
import com.digitald4.common.storage.QueryResult;
import com.digitald4.common.util.Calculate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Streams;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import javax.inject.Inject;
import javax.inject.Provider;
import org.bitbucket.cowwoc.diffmatchpatch.DiffMatchPatch.Operation;

public class InterlinearStore extends GenericStore<Interlinear, String> {
  private final ScriptureReferenceProcessor scriptureRefProcessor;
  private final InterlinearFetcher interlinearFetcher;

  @Inject
  public InterlinearStore(Provider<DAO> daoProvider,
      ScriptureReferenceProcessor scriptureRefProcessor, InterlinearFetcher interlinearFetcher) {
    super(Interlinear.class, daoProvider);
    this.scriptureRefProcessor = scriptureRefProcessor;
    this.interlinearFetcher = interlinearFetcher;
  }

  public ImmutableList<Interlinear> getInterlinear(String scriptureReference) {
    return scriptureRefProcessor.computeVerseRanges(scriptureReference).stream()
        .flatMap(vr -> getInterlinear(vr).stream())
        .collect(toImmutableList());
  }

  public ImmutableList<Interlinear> getInterlinear(VerseRange vr) {
    ImmutableList<Interlinear> interlinears = list(
        Query.forList()
            .setFilters(
                Filter.of("book", vr.getBook().name()),
                Filter.of("chapter", vr.getChapter()),
                Filter.of("verse", ">=", vr.getStartVerse()),
                Filter.of("verse", "<=", vr.getEndVerse()))
            .setOrderBys(OrderBy.of("verse")))
        .getItems().stream()
        .sorted(comparing(Interlinear::getVerse).thenComparing(Interlinear::getIndex))
        .collect(toImmutableList());

    if (interlinears.isEmpty()) {
      interlinears = create(interlinearFetcher.fetchInterlinear(vr.getBook(), vr.getChapter()))
          .stream()
          .filter(i -> i.getVerse() >= vr.getStartVerse() && i.getVerse() <= vr.getEndVerse())
          .collect(toImmutableList());
    }

    return interlinears;
  }

  public QueryResult<Interlinear> getMatchingReferences(
      String strongsId, String word, String hebrewWord, int pageSize, int pageToken) {

    Stream<Interlinear> resultsStream;
    if (strongsId != null && word != null && hebrewWord != null) {
      resultsStream = Streams.concat(
          list(Query.forList(Filter.of("strongsId", strongsId))).getItems().stream(),
          list(Query.forList(Filter.of("constantsOnly", hebrewWord))).getItems().stream());
    } else if (strongsId != null && word != null) {
      resultsStream = list(
          Query.forList(
              Filter.of("strongsId", strongsId), Filter.of("word", word))).getItems().stream();
    } else if (strongsId != null && hebrewWord != null) {
      resultsStream = list(
          Query.forList(
              Filter.of("strongsId", strongsId), 
              Filter.of("constantsOnly", hebrewWord))).getItems().stream();
    } else if (strongsId != null) {
      resultsStream = list(Query.forList(Filter.of("strongsId", strongsId))).getItems().stream();
    } else if (word != null) {
      resultsStream = list(Query.forList(Filter.of("word", word))).getItems().stream();
    } else {
      resultsStream =
          list(Query.forList(Filter.of("constantsOnly", hebrewWord))).getItems().stream();
    }

    ImmutableSet<Interlinear> results = resultsStream
        .map(i -> i.setMatchValue(getMatchValue(i, strongsId, word, hebrewWord)))
        .sorted(
            comparing(Interlinear::matchValue).reversed()
                .thenComparing(Interlinear::getBookNumber).thenComparing(Interlinear::getChapter)
                .thenComparing(Interlinear::getVerse).thenComparing(Interlinear::getIndex))
        .collect(toImmutableSet());

    return QueryResult.of(
        results.stream().skip(pageSize * (pageToken - 1)).limit(pageSize).collect(toImmutableList()),
        results.size(),
        Query.forList().setPageSize(pageSize).setPageToken(pageToken));
  }

  private static int getMatchValue(
      Interlinear interlinear, String strongsId, String word, String hebrewWord) {
    return (Objects.equals(interlinear.getStrongsId(), strongsId) ? 4 : 0)
        + (Objects.equals(interlinear.getWord(), word) ? 2 : 0)
        + (Objects.equals(interlinear.getConstantsOnly(), hebrewWord) ? 1 : 0);
  }

  public static InterlinearScripture fillDss(InterlinearScripture scripture, String dss) {
    if (dss == null) {
      return scripture;
    }

    ImmutableList<Interlinear> interlinears = scripture.getInterlinears();

    String[] dssSplit = dss.split(" ");
    if (dssSplit.length != interlinears.size()) {
      dssSplit = matchWordLength(dss,
          interlinears.stream().map(Interlinear::getConstantsOnly).collect(joining(" "))).split(" ");
    }

    String[] words = dssSplit;
    IntStream.range(0, Math.min(interlinears.size(), words.length)).forEach(index ->
        interlinears.get(index)
            .setDss(words[index].replaceAll("_", " "))
            .setDssDiff(
                Calculate.getDiffHtml(
                    words[index].equals("-") ? "" : words[index].replaceAll("_", " "),
                    interlinears.get(index).getConstantsOnly())));

    return scripture;
  }

  public static String matchWordLength(String original, String modified) {
    AtomicReference<String> previous = new AtomicReference<>();
    AtomicBoolean splitDetected = new AtomicBoolean();
    return Calculate.getDiff(original, modified).stream()
        .map(diff -> {
          if (diff.operation == Operation.DELETE && diff.text.equals(" ")) {
            return "_";
          } else if (diff.operation == Operation.DELETE && diff.text.contains(" ")) {
            return "";
          } else if (diff.operation == Operation.INSERT && diff.text.equals(" ")) {
            splitDetected.set(true);
            return "";
          } else if (diff.operation == Operation.INSERT && diff.text.contains(" ")) {
            String append = (previous.get() != null && previous.get().endsWith(" ")) ? "- " : " -";
            return diff.text.chars().mapToObj(c -> c == ' ' ? append : "").collect(joining());
          } else if (diff.operation == Operation.INSERT) {
            return "";
          } else if (splitDetected.get()) {
            splitDetected.set(false);
            return diff.text.contains(" ") ? diff.text.replaceFirst(" ", " - ") : diff.text + " -";
          }
          return diff.text;
        })
        .peek(previous::set)
        .collect(joining());
  }
}
