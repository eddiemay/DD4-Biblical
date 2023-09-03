package com.digitald4.biblical.store;

import static com.google.common.collect.ImmutableMap.toImmutableMap;
import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;

import com.digitald4.biblical.model.BibleBook;
import com.digitald4.biblical.model.ScriptureVersion;
import com.digitald4.common.exception.DD4StorageException;
import com.digitald4.common.exception.DD4StorageException.ErrorCode;
import com.digitald4.common.storage.DAO;
import com.digitald4.common.storage.GenericStore;
import com.digitald4.common.storage.Query;
import com.digitald4.common.util.Pair;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.Map;
import java.util.Optional;
import javax.inject.Inject;
import javax.inject.Provider;

public class BibleBookStore extends GenericStore<BibleBook, String> {
  @Inject
  public BibleBookStore(Provider<DAO> daoProvider) {
    super(BibleBook.class, daoProvider);
  }

  private ImmutableList<BibleBook> allBooks;
  private ImmutableMap<String, BibleBook> byName;
  private Map<String, ImmutableSet<BibleBook>> byTag;

  public BibleBook get(String name) {
    getAllBooks();
    BibleBook book = byName.get(collapseName(name));
    if (book == null) {
      throw new DD4StorageException(
          "Unknown Bible book: " + name, DD4StorageException.ErrorCode.BAD_REQUEST);
    }

    return book;
  }

  public ImmutableList<BibleBook> getAllBooks() {
    if (allBooks == null) {
      synchronized (this) {
        if (allBooks == null) {
          allBooks = list(Query.forList()).getItems();
          byName = allBooks.stream()
              .flatMap(book ->
                  ImmutableSet.<String>builder().add(book.name()).addAll(book.getAltNames()).build()
                      .stream()
                      .map(name -> Pair.of(collapseName(name), book)))
              .collect(toImmutableMap(Pair::getLeft, Pair::getRight));
          byTag = allBooks.stream()
              .flatMap(book -> book.tags().stream().map(tag -> Pair.of(tag, book)))
              .collect(groupingBy(Pair::getLeft, mapping(Pair::getRight, toImmutableSet())));
        }
      }
    }
    return allBooks;
  }

  private ImmutableSet<BibleBook> getBooks(String tag) {
    getAllBooks();
    ImmutableSet<BibleBook> books = byTag.getOrDefault(tag, ImmutableSet.of());
    return !books.isEmpty() ? books : ImmutableSet.of(get(tag));
  }

  private static String collapseName(String name) {
    return name.toLowerCase().replace(" ", "");
  }

  public BibleBook get(String name, int chapter) {
    BibleBook book = get(name);

    if (book.name().equals(BibleBook.ESTHER) && chapter > 10) {
      return get(BibleBook.ADDITIONS_TO_ESTHER);
    } else if (book.name().equals(BibleBook.ADDITIONS_TO_ESTHER) && chapter < 10) {
      return get(BibleBook.ESTHER);
    }

    if (book.name().equals(BibleBook.Psalms) && chapter > 150) {
      return get(BibleBook.APOCRYPHAL_PSALMS);
    }

    return book;
  }

  public ImmutableSet<BibleBook> getBibleBooks(String version) {
    return ScriptureVersion.get(version).getTags().stream()
        .flatMap(tag -> getBooks(tag).stream())
        .collect(toImmutableSet());
  }

  public boolean meetsCriteria(String version, BibleBook book, String lang) {
    ScriptureVersion scriptureVersion = ScriptureVersion.get(version);
    return getBibleBooks(version).contains(book)
        && (lang == null || scriptureVersion.getSupportedLanguages().contains(lang));
  }

  public ScriptureVersion getOrFallback(
      String version, String lang, BibleBook book, boolean required) {
    ScriptureVersion scriptureVersion = ScriptureVersion.get(version);
    if (scriptureVersion == null) {
      throw new DD4StorageException("Unknown scripture version: " + version, ErrorCode.BAD_REQUEST);
    }

    if (meetsCriteria(version, book, lang)) {
      return scriptureVersion;
    }

    Optional<ScriptureVersion> fallback =
        ScriptureVersion.BY_VERSION.values().stream()
            .filter(sv -> meetsCriteria(sv.getVersion(), book, lang)).findFirst();

    return required
        ? fallback.orElseThrow(
        () -> new DD4StorageException(
            "No source found for book: " + book + " in language: " + lang,
            ErrorCode.BAD_REQUEST))
        : fallback.orElse(null);
  }
}
