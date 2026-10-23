package twitter;

import static org.junit.Assert.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

public class FilterTest {

    /*
     * Testing strategy
     *
     * writtenBy(tweets, username):
     *   number of tweets in list: 0, 1, many
     *   tweets by the user: 0, 1, many
     *   case of username vs. author: same case, different case
     *   order of result: must match order of input
     *
     * inTimespan(tweets, timespan):
     *   number of tweets in list: 0, 1, many
     *   tweets inside the timespan: 0, some, all
     *   tweet exactly on the start / end boundary: yes, no
     *   order of result: must match order of input
     *
     * containing(tweets, words):
     *   number of tweets in list: 0, 1, many
     *   number of words searched: 1, many
     *   matching tweets: 0, 1, many
     *   case of word vs. text: same case, different case
     *   word is only part of a longer word (e.g. "talk" in "talking"): yes, no
     *   word touching punctuation or hashtag in text: yes, no
     *   order of result: must match order of input
     */

    private static final Instant d1 = Instant.parse("2016-02-17T10:00:00Z");
    private static final Instant d2 = Instant.parse("2016-02-17T11:00:00Z");
    private static final Instant d3 = Instant.parse("2016-02-17T12:00:00Z");

    private static final Tweet tweet1 = new Tweet(1, "alyssa", "is it reasonable to talk about rivest so much?", d1);
    private static final Tweet tweet2 = new Tweet(2, "bbitdiddle", "rivest talk in 30 minutes #hype", d2);
    private static final Tweet tweet3 = new Tweet(3, "Alyssa", "Talking about Rivest again", d3);

    @Test(expected=AssertionError.class)
    public void testAssertionsEnabled() {
        assert false; // make sure assertions are enabled with VM argument: -ea
    }

    // ---------------- writtenBy ----------------

    @Test
    public void testWrittenByMultipleTweetsSingleResult() {
        List<Tweet> writtenBy = Filter.writtenBy(Arrays.asList(tweet1, tweet2), "alyssa");

        assertEquals("expected singleton list", 1, writtenBy.size());
        assertTrue("expected list to contain tweet", writtenBy.contains(tweet1));
    }

    @Test
    public void testWrittenByEmptyList() {
        List<Tweet> writtenBy = Filter.writtenBy(new ArrayList<Tweet>(), "alyssa");

        assertTrue("expected empty list", writtenBy.isEmpty());
    }

    @Test
    public void testWrittenByNoMatch() {
        List<Tweet> writtenBy = Filter.writtenBy(Arrays.asList(tweet1, tweet2), "nobody");

        assertTrue("expected empty list", writtenBy.isEmpty());
    }

    @Test
    public void testWrittenByMultipleResultsKeepsOrder() {
        List<Tweet> writtenBy = Filter.writtenBy(Arrays.asList(tweet1, tweet2, tweet3), "alyssa");

        assertEquals("expected two tweets", 2, writtenBy.size());
        assertEquals("expected same order", Arrays.asList(tweet1, tweet3), writtenBy);
    }

    @Test
    public void testWrittenByDifferentCase() {
        List<Tweet> writtenBy = Filter.writtenBy(Arrays.asList(tweet2), "BBitDiddle");

        assertEquals("usernames are case-insensitive", Arrays.asList(tweet2), writtenBy);
    }

    // ---------------- inTimespan ----------------

    @Test
    public void testInTimespanMultipleTweetsMultipleResults() {
        Instant testStart = Instant.parse("2016-02-17T09:00:00Z");
        Instant testEnd = Instant.parse("2016-02-17T12:00:00Z");

        List<Tweet> inTimespan = Filter.inTimespan(Arrays.asList(tweet1, tweet2), new Timespan(testStart, testEnd));

        assertFalse("expected non-empty list", inTimespan.isEmpty());
        assertTrue("expected list to contain tweets", inTimespan.containsAll(Arrays.asList(tweet1, tweet2)));
        assertEquals("expected same order", 0, inTimespan.indexOf(tweet1));
    }

    @Test
    public void testInTimespanEmptyList() {
        List<Tweet> inTimespan = Filter.inTimespan(new ArrayList<Tweet>(), new Timespan(d1, d3));

        assertTrue("expected empty list", inTimespan.isEmpty());
    }

    @Test
    public void testInTimespanNoneInside() {
        Instant start = Instant.parse("2016-02-17T13:00:00Z");
        Instant end = Instant.parse("2016-02-17T14:00:00Z");

        List<Tweet> inTimespan = Filter.inTimespan(Arrays.asList(tweet1, tweet2), new Timespan(start, end));

        assertTrue("expected empty list", inTimespan.isEmpty());
    }

    @Test
    public void testInTimespanSomeInside() {
        Instant start = Instant.parse("2016-02-17T10:30:00Z");
        Instant end = Instant.parse("2016-02-17T11:30:00Z");

        List<Tweet> inTimespan = Filter.inTimespan(Arrays.asList(tweet1, tweet2, tweet3), new Timespan(start, end));

        assertEquals("only tweet2 is inside", Arrays.asList(tweet2), inTimespan);
    }

    @Test
    public void testInTimespanBoundaryIncluded() {
        // tweet1 is exactly at start, tweet3 exactly at end
        List<Tweet> inTimespan = Filter.inTimespan(Arrays.asList(tweet1, tweet2, tweet3), new Timespan(d1, d3));

        assertEquals("boundaries are included, order kept", Arrays.asList(tweet1, tweet2, tweet3), inTimespan);
    }

    // ---------------- containing ----------------

    @Test
    public void testContaining() {
        List<Tweet> containing = Filter.containing(Arrays.asList(tweet1, tweet2), Arrays.asList("talk"));

        assertFalse("expected non-empty list", containing.isEmpty());
        assertTrue("expected list to contain tweets", containing.containsAll(Arrays.asList(tweet1, tweet2)));
        assertEquals("expected same order", 0, containing.indexOf(tweet1));
    }

    @Test
    public void testContainingEmptyList() {
        List<Tweet> containing = Filter.containing(new ArrayList<Tweet>(), Arrays.asList("talk"));

        assertTrue("expected empty list", containing.isEmpty());
    }

    @Test
    public void testContainingNoMatch() {
        List<Tweet> containing = Filter.containing(Arrays.asList(tweet1, tweet2), Arrays.asList("zebra"));

        assertTrue("expected empty list", containing.isEmpty());
    }

    @Test
    public void testContainingDifferentCase() {
        List<Tweet> containing = Filter.containing(Arrays.asList(tweet1, tweet3), Arrays.asList("RIVEST"));

        assertEquals("case must be ignored", Arrays.asList(tweet1, tweet3), containing);
    }

    @Test
    public void testContainingManyWordsOneMatchEnough() {
        List<Tweet> containing = Filter.containing(Arrays.asList(tweet1, tweet2, tweet3), Arrays.asList("zebra", "#hype"));

        assertEquals("only tweet2 has #hype", Arrays.asList(tweet2), containing);
    }

    @Test
    public void testContainingPartOfWordDoesNotMatch() {
        // "talk" is inside "Talking" but is not a separate word
        List<Tweet> containing = Filter.containing(Arrays.asList(tweet3), Arrays.asList("talk"));

        assertTrue("partial word must not match", containing.isEmpty());
    }

    @Test
    public void testContainingWordBoundedByEndsOfString() {
        List<Tweet> containing = Filter.containing(Arrays.asList(tweet3), Arrays.asList("talking", "again"));

        assertEquals("first and last word of the text match", Arrays.asList(tweet3), containing);
    }

    @Test
    public void testContainingPunctuationAttachedDoesNotMatch() {
        // "much?" is the word in tweet1, not "much"
        List<Tweet> containing = Filter.containing(Arrays.asList(tweet1), Arrays.asList("much"));

        assertTrue("word with attached punctuation is a different word", containing.isEmpty());
    }

}
