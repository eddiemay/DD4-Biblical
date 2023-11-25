package com.digitald4.biblical.store;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.truth.Truth.assertThat;
import static java.util.stream.Collectors.toList;

import com.digitald4.biblical.model.Interlinear;
import com.digitald4.biblical.model.Scripture.InterlinearScripture;
import com.google.common.collect.ImmutableList;
import java.util.Arrays;
import org.junit.Test;

public class InterlinearStoreTest {

  @Test
  public void fillDss_exactMatch() {
    ImmutableList<Interlinear> interlinears = createInterlinears("Hello Brown");
    InterlinearStore.fillDss(new InterlinearScripture(interlinears), "Hello Brown");
    assertThat(interlinears.stream().map(Interlinear::getDss).collect(toImmutableList()))
        .containsExactly("Hello", "Brown");
    assertThat(interlinears.stream().map(Interlinear::dssDiff).collect(toImmutableList()))
        .containsExactly("Hello", "Brown");
  }

  @Test
  public void fillDss_spellingDiff() {
    ImmutableList<Interlinear> interlinears = createInterlinears("Helo Bron");
    InterlinearStore.fillDss(new InterlinearScripture(interlinears), "Hello Brown");
    assertThat(interlinears.stream().map(Interlinear::getDss).collect(toImmutableList()))
        .containsExactly("Hello", "Brown");
    assertThat(interlinears.stream().map(Interlinear::dssDiff).collect(toImmutableList()))
        .containsExactly(
            "Hel<span class=\"diff-delete\">l</span>o", "Bro<span class=\"diff-delete\">w</span>n");
  }

  @Test
  public void fillDss_wordRemoved() {
    ImmutableList<Interlinear> interlinears = createInterlinears("Hello Brown");
    InterlinearStore.fillDss(new InterlinearScripture(interlinears), "Hello Mr. Brown");
    assertThat(interlinears.stream().map(Interlinear::getDss).collect(toImmutableList()))
        .containsExactly("Hello", "Brown");
    assertThat(interlinears.stream().map(Interlinear::dssDiff).collect(toImmutableList()))
        .containsExactly("Hello", "Brown");
  }

  @Test
  public void fillDss_wordRemovedAndSpellingDiff() {
    ImmutableList<Interlinear> interlinears = createInterlinears("Helo Bron");
    InterlinearStore.fillDss(new InterlinearScripture(interlinears), "Hello Mr. Brown");
    assertThat(interlinears.stream().map(Interlinear::getDss).collect(toImmutableList()))
        .containsExactly("Hello", "Brown");
    assertThat(interlinears.stream().map(Interlinear::dssDiff).collect(toImmutableList()))
        .containsExactly(
            "Hel<span class=\"diff-delete\">l</span>o", "Bro<span class=\"diff-delete\">w</span>n");
  }

  @Test
  public void fillDss_wordAdded() {
    ImmutableList<Interlinear> interlinears = createInterlinears("Hello James Brown");
    InterlinearStore.fillDss(new InterlinearScripture(interlinears), "Hello Brown");
    assertThat(interlinears.stream().map(Interlinear::getDss).collect(toList()))
        .containsExactly("Hello", "-", "Brown");
    assertThat(interlinears.stream().map(Interlinear::dssDiff).collect(toList()))
        .containsExactly("Hello", "<span class=\"diff-insert\">James</span>", "Brown");
  }

  @Test
  public void fillDss_multiWordsAdded() {
    ImmutableList<Interlinear> interlinears = createInterlinears("Hello Mr James Walter Brown");
    InterlinearStore.fillDss(new InterlinearScripture(interlinears), "Hello Brown");
    assertThat(interlinears.stream().map(Interlinear::getDss).collect(toList()))
        .containsExactly("Hello", "-", "-", "-", "Brown");
    assertThat(interlinears.stream().map(Interlinear::dssDiff).collect(toList())).containsExactly(
        "Hello", "<span class=\"diff-insert\">Mr</span>", "<span class=\"diff-insert\">James</span>",
        "<span class=\"diff-insert\">Walter</span>", "Brown");
  }

  @Test
  public void fillDss_wordChangedAndAdded() {
    ImmutableList<Interlinear> interlinears = createInterlinears("Hello Mrs Jamie Brown");
    InterlinearStore.fillDss(new InterlinearScripture(interlinears), "Hello Mr Brown");
    assertThat(interlinears.stream().map(Interlinear::getDss).collect(toList()))
        .containsExactly("Hello", "Mr", "-", "Brown");
    assertThat(interlinears.stream().map(Interlinear::dssDiff).collect(toList())).containsExactly(
        "Hello", "Mr<span class=\"diff-insert\">s</span>", "<span class=\"diff-insert\">Jamie</span>", "Brown");
  }

  @Test
  public void fillDss_wordSplit() {
    ImmutableList<Interlinear> interlinears = createInterlinears("Hello Immanu el");
    InterlinearStore.fillDss(new InterlinearScripture(interlinears), "Hello Immanuel");
    assertThat(interlinears.stream().map(Interlinear::getDss).collect(toList()))
        .containsExactly("Hello", "Immanuel", "-");
    assertThat(interlinears.stream().map(Interlinear::dssDiff).collect(toList())).containsExactly(
        "Hello", "Immanu<span class=\"diff-delete\">el</span>", "<span class=\"diff-insert\">el</span>");
  }

  @Test
  public void fillDss_wordsCombined() {
    ImmutableList<Interlinear> interlinears = createInterlinears("watch basketball game");
    InterlinearStore.fillDss(new InterlinearScripture(interlinears), "watch basket ball game");
    assertThat(interlinears.stream().map(Interlinear::getDss).collect(toList()))
        .containsExactly("watch", "basket ball", "game");
    assertThat(interlinears.stream().map(Interlinear::dssDiff).collect(toList())).containsExactly(
        "watch", "basket<span class=\"diff-delete\"> </span>ball", "game");
  }

  @Test
  public void fillDss_wordSplitWithTextAfter() {
    ImmutableList<Interlinear> interlinears = createInterlinears("Hello Immanu el Lewis");
    InterlinearStore.fillDss(new InterlinearScripture(interlinears), "Hello Immanuel Lewis");
    assertThat(interlinears.stream().map(Interlinear::getDss).collect(toList()))
        .containsExactly("Hello", "Immanuel", "-", "Lewis");
    assertThat(interlinears.stream().map(Interlinear::dssDiff).collect(toList())).containsExactly(
        "Hello", "Immanu<span class=\"diff-delete\">el</span>",
        "<span class=\"diff-insert\">el</span>", "Lewis");
  }

  @Test
  public void matchWordLength_equal() {
    assertThat(InterlinearStore.matchWordLength("Hello Brown", "Hello Brown"))
        .isEqualTo("Hello Brown");
  }

  @Test
  public void matchWordLength_spellingDiff() {
    assertThat(InterlinearStore.matchWordLength("Hello Brown", "Helo Bron"))
        .isEqualTo("Hello Brown");
  }

  @Test
  public void matchWordLength_wordRemoved() {
    assertThat(InterlinearStore.matchWordLength("Hello Mr. Brown", "Hello Brown"))
        .isEqualTo("Hello Brown");
  }

  @Test
  public void matchWordLength_wordAdded() {
    assertThat(InterlinearStore.matchWordLength("Hello Brown", "Hello James Brown"))
        .isEqualTo("Hello - Brown");
  }

  @Test
  public void matchWordLength_multiWordsAdded() {
    assertThat(InterlinearStore.matchWordLength("Hello Brown", "Hello Mr James Walter Brown"))
        .isEqualTo("Hello - - - Brown");
  }

  @Test
  public void matchWordLength_spellingDiffAndWordAdded() {
    assertThat(InterlinearStore.matchWordLength("Hello Mr Brown", "Hello Mrs Jamie Brown"))
        .isEqualTo("Hello Mr - Brown");
  }

  @Test
  public void matchWordLength_wordSplit() {
    assertThat(InterlinearStore.matchWordLength("Hello Immanuel", "Hello Immanu el"))
        .isEqualTo("Hello Immanuel -");
  }

  @Test
  public void matchWordLength_wordSplitWithTextAfter() {
    assertThat(InterlinearStore.matchWordLength("Hello Immanuel Lewis", "Hello Immanu el Lewis"))
        .isEqualTo("Hello Immanuel - Lewis");
  }

  @Test
  public void matchWordLength_wordAddedAndSpellingDiff() {
    assertThat(InterlinearStore.matchWordLength("Hello Brown", "Helo James Bron"))
        .isEqualTo("Hello - Brown");
  }

  @Test
  public void matchWordLength_wordAddedAndRemovedAndSpellingDiff() {
    assertThat(InterlinearStore.matchWordLength("Hello Brown, How are you doing?", "Helo James Bron, How you doing?"))
        .isEqualTo("Hello - Brown, How you doing?");
  }

  private static ImmutableList<Interlinear> createInterlinears(String wlc) {
    return Arrays.stream(wlc.split(" "))
        .map(word -> new Interlinear().setConstantsOnly(word))
        .collect(toImmutableList());
  }
}
