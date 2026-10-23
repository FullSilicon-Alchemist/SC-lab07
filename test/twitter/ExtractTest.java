package twitter;

import static org.junit.Assert.*;

import java.time.Instant;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

public class ExtractTest {

    /*
     * Testing strategy
     *
     * getTimespan(tweets):
     *   number of tweets: 1, 2, many
     *   order of timestamps: ascending, descending, unordered
     *   same timestamp in two tweets: yes, no
     *
     * getMentionedUsers(tweets):
     *   number of mentions in a tweet: 0, 1, many
     *   same user mentioned twice (different case): yes, no
     *   email address in text: yes, no
     *   punctuation right after the name (like "@bob,"): yes, no
     *   number of tweets: 1, many
     *
     * Note: the spec leaves the case of returned usernames open, so the
     * mention tests lower-case the result before comparing.
     */

    private static final Instant d1 = Instant.parse("2016-02-17T10:00:00Z");
    private static final Instant d2 = Instant.parse("2016-02-17T11:00:00Z");
    private static final Instant d3 = Instant.parse("2016-02-17T12:00:00Z");

    private static final Tweet tweet1 = new Tweet(1, "alyssa", "is it reasonable to talk about rivest so much?", d1);
private static final Tweet tweet2 = new Tweet(2, "bbitdiddle", "rivest talk in 30 minutes #hype", d2);

    // helper that lives in the test class only (not in Extract)
    private static Set<String> lower(Set<String> users) {
        Set<String> result = new HashSet<>();
        for (String u : users) {
            result.add(u.toLowerCase());
        }
        return result;
    }

    private static Set<String> setOf(String... names) {
        return new HashSet<>(Arrays.asList(names));
    }

    @Test(expected=AssertionError.class)
    public void testAssertionsEnabled() {
        assert false; // make sure assertions are enabled with VM argument: -ea
    }

    // ---------------- getTimespan ----------------

    @Test
    public void testGetTimespanOneTweet() {
        Timespan timespan = Extract.getTimespan(Arrays.asList(tweet1));

        assertEquals("expected start", d1, timespan.getStart());
        assertEquals("expected end", d1, timespan.getEnd());
    }

    @Test
    public void testGetTimespanTwoTweets() {
        Timespan timespan = Extract.getTimespan(Arrays.asList(tweet1, tweet2));

        assertEquals("expected start", d1, timespan.getStart());
        assertEquals("expected end", d2, timespan.getEnd());
    }

    @Test
    public void testGetTimespanTwoTweetsDescending() {
        Timespan timespan = Extract.getTimespan(Arrays.asList(tweet2, tweet1));

        assertEquals("expected start", d1, timespan.getStart());
        assertEquals("expected end", d2, timespan.getEnd());
    }

    @Test
    public void testGetTimespanManyTweetsUnordered() {
        Tweet tweet3 = new Tweet(3, "ali", "hello", d3);
        Timespan timespan = Extract.getTimespan(Arrays.asList(tweet2, tweet3, tweet1));

        assertEquals("expected start", d1, timespan.getStart());
        assertEquals("expected end", d3, timespan.getEnd());
    }

    @Test
    public void testGetTimespanSameTimestamp() {
        Tweet tweet3 = new Tweet(3, "ali", "hello", d1);
        Timespan timespan = Extract.getTimespan(Arrays.asList(tweet1, tweet3));

        assertEquals("expected start", d1, timespan.getStart());
        assertEquals("expected end", d1, timespan.getEnd());
}

    // ---------------- getMentionedUsers ----------------

    @Test
    public void testGetMentionedUsersNoMention() {
        Set<String> mentionedUsers = Extract.getMentionedUsers(Arrays.asList(tweet1));

        assertTrue("expected empty set", mentionedUsers.isEmpty());
    }

    @Test
    public void testGetMentionedUsersOneMention() {
        Tweet t = new Tweet(3, "ali", "hi @sara how are you", d1);
        Set<String> mentionedUsers = Extract.getMentionedUsers(Arrays.asList(t));

        assertEquals("expected one user", setOf("sara"), lower(mentionedUsers));
    }

    @Test
    public void testGetMentionedUsersManyMentions() {
        Tweet t = new Tweet(3, "ali", "@sara and @bob_1 and @x-y are here", d1);
        Set<String> mentionedUsers = Extract.getMentionedUsers(Arrays.asList(t));

        assertEquals("expected three users", setOf("sara", "bob_1", "x-y"), lower(mentionedUsers));
    }

    @Test
    public void testGetMentionedUsersSameUserDifferentCase() {
        Tweet t = new Tweet(3, "ali", "@Sara hello @sara", d1);
        Set<String> mentionedUsers = Extract.getMentionedUsers(Arrays.asList(t));

        assertEquals("user should appear only once", 1, mentionedUsers.size());
        assertEquals("expected sara", setOf("sara"), lower(mentionedUsers));
    }

    @Test
    public void testGetMentionedUsersEmailIsNotMention() {
        Tweet t = new Tweet(3, "ali", "mail bitdiddle@mit.edu now", d1);
        Set<String> mentionedUsers = Extract.getMentionedUsers(Arrays.asList(t));

        assertTrue("email address is not a mention", mentionedUsers.isEmpty());
    }

    @Test
    public void testGetMentionedUsersEmailAndRealMention() {
        Tweet t = new Tweet(3, "ali", "bob@mit.edu and hi @Sara and @sara", d1);
        Set<String> mentionedUsers = Extract.getMentionedUsers(Arrays.asList(t));

        assertEquals("only sara is mentioned", setOf("sara"), lower(mentionedUsers));
    }

    @Test
    public void testGetMentionedUsersPunctuationAfterName() {
        Tweet t = new Tweet(3, "ali", "@sara, meet @bob.", d1);
        Set<String> mentionedUsers = Extract.getMentionedUsers(Arrays.asList(t));

        assertEquals("expected sara and bob", setOf("sara", "bob"), lower(mentionedUsers));
    }

    @Test
    public void testGetMentionedUsersManyTweets() {
 Tweet t3 = new Tweet(3, "ali", "hi @sara", d1);
        Tweet t4 = new Tweet(4, "ali", "bye @bob and @Sara", d2);
        Set<String> mentionedUsers = Extract.getMentionedUsers(Arrays.asList(t3, t4));

        assertEquals("expected sara and bob once each", setOf("sara", "bob"), lower(mentionedUsers));
        assertEquals("no duplicates", 2, mentionedUsers.size());
    }

}
