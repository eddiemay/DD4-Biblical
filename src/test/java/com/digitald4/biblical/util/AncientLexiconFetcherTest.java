package com.digitald4.biblical.util;

import static com.digitald4.biblical.util.ScriptureFetcherTest.getContent;
import static com.google.common.truth.Truth.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.digitald4.biblical.model.AncientLexicon;
import com.digitald4.common.server.APIConnector;
import com.google.common.collect.ImmutableSet;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mock;

public class AncientLexiconFetcherTest {
  @Mock
  private final APIConnector apiConnector = mock(APIConnector.class);
  private AncientLexiconFetcher lexiconFetcher;

  @Before
  public void setup() {
    lexiconFetcher = new AncientLexiconFetcher(apiConnector);
  }

  @Test @Ignore
  public void fetchAyin() throws Exception {
    when(apiConnector.sendGet(anyString())).thenReturn(
        getContent("src/test/java/com/digitald4/biblical/util/data/ancient-hebrew-ayin.html"));

    assertThat(lexiconFetcher.fetch(9).subList(0, 18)).containsExactly(
        new AncientLexicon().setWord("עב"),
        new AncientLexicon().setWord("עב").setTranslation("BEAM")
            .setKjvTranslations(ImmutableSet.of("plank", "beam"))
            .setStrongIds(ImmutableSet.of("H5646")),
        new AncientLexicon().setWord("עבד").setDefinition(
            "A work performed or made for another out of obligation, requirement or gratitude."),
        new AncientLexicon().setWord("עבד").setTranslation("SERVE (V)").setDefinition(
            "To provide a service to another, as a servant or slave or to work at a profession.")
            .setKjvTranslations(
                ImmutableSet.of("serve", "do", "till", "servant", "work", "worshipper", "service",
                    "dress", "labour", "ear", "make", "go", "keep", "move", "wrought"))
            .setStrongIds(ImmutableSet.of("H5647", "A5648")),
        new AncientLexicon().setWord("עבד").setTranslation("SERVANT").setDefinition(
            "One who provides a service to another, as a slave, bondservant or hired hand.")
            .setKjvTranslations(
                ImmutableSet.of(
                    "servant", "manservant", "bondman", "bondage", "bondservant", "sides"))
            .setStrongIds(ImmutableSet.of("H5650", "H5652", "H5657", "A5649")),
        new AncientLexicon().setWord("עבדות").setTranslation("SERVITUDE")
            .setDefinition("[To be verified] A forced service.")
            .setKjvTranslations(ImmutableSet.of("bondage"))
            .setStrongIds(ImmutableSet.of("H5659")),
        new AncientLexicon().setWord("מעבד").setTranslation("SERVICE")
            .setKjvTranslations(ImmutableSet.of("work"))
            .setStrongIds(ImmutableSet.of("H4566", "A4567")),
        new AncientLexicon().setWord("עבידה").setTranslation("SERVICE")
            .setKjvTranslations(ImmutableSet.of("work", "affair", "service"))
            .setWords(ImmutableSet.of("עבידא"))
            .setStrongIds(ImmutableSet.of("A5673")),
        new AncientLexicon().setWord("עבודה").setTranslation("SERVICE")
            .setDefinition("Labor provided by a servant or slave.")
            .setKjvTranslations(
                ImmutableSet.of("service", "servile", "work", "bondage", "act", "serve",
                    "servitude", "tillage", "effect", "labour"))
            .setStrongIds(ImmutableSet.of("H5656")),
        new AncientLexicon().setWord("עבט").setDefinition("When something is borrowed the borrower"
            + " gives an item as a pledge as a security for the return of what is borrowed."),
        new AncientLexicon().setWord("עבט").setTranslation("MAKE.A.PLEDGE (V)")
            .setKjvTranslations(ImmutableSet.of("lend", "fetch", "borrow", "break"))
            .setStrongIds(ImmutableSet.of("H5670")),
        new AncientLexicon().setWord("עבוט").setTranslation("PLEDGE")
            .setDefinition("What is given as security for a loan.")
            .setKjvTranslations(ImmutableSet.of("pledge"))
            .setStrongIds(ImmutableSet.of("H5667")),
        new AncientLexicon().setWord("עבטיט").setTranslation("PLEDGE")
            .setDefinition("[To be verified] What is given as security for a loan.")
            .setKjvTranslations(ImmutableSet.of("clay")).setStrongIds(ImmutableSet.of("H5671")),
        new AncientLexicon().setWord("עג"),
        new AncientLexicon().setWord("עוג").setDefinition("Cakes baked on hot stones."),
        new AncientLexicon().setWord("עוג").setTranslation("BAKE (V)")
            .setKjvTranslations(ImmutableSet.of("bake")).setStrongIds(ImmutableSet.of("H5746")),
        new AncientLexicon().setWord("עוג").setWords(ImmutableSet.of("עוגה"))
            .setTranslation("BREAD.CAKE").setDefinition("A bread that is baked on hot stones.")
            .setKjvTranslations(ImmutableSet.of("cake")).setStrongIds(ImmutableSet.of("H5692")),
        new AncientLexicon().setWord("מעוג").setTranslation("CAKE")
            .setKjvTranslations(ImmutableSet.of("cake", "feast"))
            .setStrongIds(ImmutableSet.of("H4580"))
    );
  }
}
