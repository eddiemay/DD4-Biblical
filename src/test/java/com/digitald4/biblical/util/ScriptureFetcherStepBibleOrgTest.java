package com.digitald4.biblical.util;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.digitald4.biblical.model.Scripture;
import org.junit.Test;

public class ScriptureFetcherStepBibleOrgTest extends ScriptureFetcherTest {
  @Test
  public void fetch() {
    when(apiConnector.sendGet(anyString())).thenReturn(
        """
            {
                "time": 0,
                "signature": "reference=Ps.117|version=RSKJ-NONE-",
                "searchType": "PASSAGE",
                "masterVersion": "RSKJ",
                "extraVersions": "",
                "interlinearMode": "NONE",
                "searchTokens": [
                    {
                        "enhancedTokenInfo": {
                            "initials": "RNKJV",
                            "hasStrongs": false,
                            "hasMorphology": false,
                            "hasRedLetter": false,
                            "hasNotes": false,
                            "hasHeadings": false,
                            "questionable": false,
                            "shortInitials": "RSKJ",
                            "hasSeptuagintTagging": false
                        },
                        "token": "RSKJ",
                        "tokenType": "version"
                    },
                    {
                        "enhancedTokenInfo": {
                            "sectionType": "PASSAGE",
                            "shortName": "Psa 117",
                            "fullName": "Psa 117",
                            "wholeBook": false,
                            "osisID": "Ps.117",
                            "passage": false
                        },
                        "token": "Ps.117",
                        "tokenType": "reference"
                    }
                ],
                "timeTookTotal": 14,
                "previousChapter": {
                    "osisKeyId": "Ps.116",
                    "name": "Psa 116",
                    "lastChapter": false
                },
                "nextChapter": {
                    "osisKeyId": "Ps.118",
                    "name": "Psa 118",
                    "lastChapter": false
                },
                "value": "<div tabindex='-1' class='passageContentHolder'><h2 class='xgen'>Psalms 117</h2> <span dir='ltr' class='verse ltrDirection'><a name='Ps.117.1' class='verseLink'><span class='verseNumber'>1</span></a>O praise <span class='small-caps'> יהוה</span>, all ye nations: praise him, all ye people. </span> <span dir='ltr' class='verse ltrDirection'><a name='Ps.117.2' class='verseLink'><span class='verseNumber'>2</span></a>For his merciful kindness is great toward us: and the truth of <span class='small-caps'> יהוה</span> endureth for ever. Praise ye <span class='small-caps'> יהוה</span>.  </span> </div>",
                "reference": "Psa 117",
                "osisId": "Ps.117",
                "fragment": false,
                "multipleRanges": false,
                "startRange": 16484,
                "endRange": 16485,
                "languageCode": [
                    "en"
                ],
                "longName": "Psalms 117",
                "options": "VL",
                "selectedOptions": "VNHUG",
                "removedOptions": [
                    {
                        "explanation": "This option is not available in the currently selected text.",
                        "option": "N"
                    },
                    {
                        "explanation": "This option is not available in the currently selected text.",
                        "option": "H"
                    },
                    {
                        "explanation": "This option is not available in the currently selected text.",
                        "option": "U"
                    },
                    {
                        "explanation": "This option is not available in the currently selected text.",
                        "option": "G"
                    }
                ]
            }""");

    assertThat(scriptureStore.getScriptures("RSKJ", Language.EN, "Psalms 117").getItems()).containsExactly(
        new Scripture().setVersion("RSKJ").setBook("Psalms").setChapter(117).setVerse(1).setText(
            "O praise יהוה, all ye nations: praise him, all ye people."),
        new Scripture().setVersion("RSKJ").setBook("Psalms").setChapter(117).setVerse(2).setText(
            "For his merciful kindness is great toward us: and the truth of יהוה endureth for ever. Praise ye יהוה."));
  }
}
