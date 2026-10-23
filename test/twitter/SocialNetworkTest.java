/* Copyright (c) 2007-2016 MIT 6.005 course staff, all rights reserved.
 * Redistribution of original or derived work requires permission of course staff.
 */
package twitter;

import static org.junit.Assert.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Test;

public class SocialNetworkTest {

    /*
     * Testing strategy for guessFollowsGraph(tweets):
     *   number of tweets: 0, 1, > 1
     *   mentions in a tweet: 0, 1, > 1
     *   tweets by the same author: 1, > 1 (with different mentions)
     *   case of a mentioned username: same as elsewhere, different
     *
     * The spec is underdetermined: the graph may use any case for usernames,
     * may contain a user with an empty set, and may use extra evidence.
     * So the tests only check the follows that MUST be there (A @-mentions B),
     * and compare usernames while ignoring case.
     *
     * Testing strategy for influencers(followsGraph):
     *   number of users in the graph: 0, 1, > 1
     *   followers of the top user: 0, 1, > 1
     *   follower counts: all different, some tied
     */

    private static final Instant d1 = Instant.parse("2016-02-17T10:00:00Z");
    private static final Instant d2 = Instant.parse("2016-02-17T11:00:00Z");

    private static final Tweet noMention = new Tweet(1, "alyssa", "is it reasonable to talk about rivest so much?", d1);
    private static final Tweet oneMention = new Tweet(2, "bbitdiddle", "saving a seat for @Alyssa", d1);
    private static final Tweet twoMentions = new Tweet(3, "evaluator", "thanks @bbitdiddle and @alyssa", d2);
    private static final Tweet bbitAgain = new Tweet(4, "BBitDiddle", "@evaluator see you at lecture", d2);

    @Test(expected=AssertionError.class)
    public void testAssertionsEnabled() {
        assert false; // make sure assertions are enabled with VM argument: -ea
    }

    // ---------------- guessFollowsGraph ----------------

    // covers: 0 tweets
    @Test
    public void testGuessFollowsGraphEmpty() {
        Map<String, Set<String>> followsGraph = SocialNetwork.guessFollowsGraph(new ArrayList<>());

        assertTrue("expected empty graph", followsGraph.isEmpty());
    }

    // covers: 1 tweet, 0 mentions
    @Test
    public void testGuessFollowsGraphNoMentions() {
        Map<String, Set<String>> followsGraph =
                SocialNetwork.guessFollowsGraph(Arrays.asList(noMention));

        // alyssa may appear as a key, but she must not follow anybody
        for (Set<String> follows : followsGraph.values()) {
            assertTrue("expected no follows", follows.isEmpty());
        }
    }

    // covers: 1 tweet, 1 mention, different case
    @Test
    public void testGuessFollowsGraphSingleMention() {
        Map<String, Set<String>> followsGraph =
                SocialNetwork.guessFollowsGraph(Arrays.asList(oneMention));

        assertTrue("expected bbitdiddle follows alyssa", follows(followsGraph, "bbitdiddle", "alyssa"));
    }

    // covers: 1 tweet, > 1 mentions
    @Test
    public void testGuessFollowsGraphMultipleMentions() {
        Map<String, Set<String>> followsGraph =
                SocialNetwork.guessFollowsGraph(Arrays.asList(twoMentions));

        assertTrue("expected evaluator follows bbitdiddle", follows(followsGraph, "evaluator", "bbitdiddle"));
        assertTrue("expected evaluator follows alyssa", follows(followsGraph, "evaluator", "alyssa"));
    }

    // covers: > 1 tweets, same author in different case, different mentions
    @Test
    public void testGuessFollowsGraphMultipleTweetsOneUser() {
        Map<String, Set<String>> followsGraph =
                SocialNetwork.guessFollowsGraph(Arrays.asList(oneMention, bbitAgain));

        assertTrue("expected bbitdiddle follows alyssa", follows(followsGraph, "bbitdiddle", "alyssa"));
        assertTrue("expected bbitdiddle follows evaluator", follows(followsGraph, "bbitdiddle", "evaluator"));
    }

    // ---------------- influencers ----------------

    // covers: 0 users
    @Test
    public void testInfluencersEmpty() {
        Map<String, Set<String>> followsGraph = new HashMap<>();
        List<String> influencers = SocialNetwork.influencers(followsGraph);

        assertTrue("expected empty list", influencers.isEmpty());
    }

    // covers: 1 user, top user has 0 followers
    @Test
    public void testInfluencersSingleUserNoFollowers() {
        Map<String, Set<String>> followsGraph = new HashMap<>();
        followsGraph.put("alyssa", new HashSet<>());

        List<String> influencers = lower(SocialNetwork.influencers(followsGraph));

        // the spec says the list holds ALL usernames in the graph
        assertEquals("expected the one user", Arrays.asList("alyssa"), influencers);
    }

    // covers: > 1 users, top user has 1 follower
    @Test
    public void testInfluencersOneFollower() {
        Map<String, Set<String>> followsGraph = new HashMap<>();
        followsGraph.put("bbitdiddle", new HashSet<>(Arrays.asList("alyssa")));

        List<String> influencers = lower(SocialNetwork.influencers(followsGraph));

        assertEquals("expected alyssa (1 follower) before bbitdiddle (0 followers)",
                Arrays.asList("alyssa", "bbitdiddle"), influencers);
    }

    // covers: > 1 users, top user has > 1 followers, all counts different
    @Test
    public void testInfluencersManyFollowersDifferentCounts() {
        Map<String, Set<String>> followsGraph = new HashMap<>();
        followsGraph.put("bbitdiddle", new HashSet<>(Arrays.asList("alyssa")));
        followsGraph.put("evaluator", new HashSet<>(Arrays.asList("alyssa", "bbitdiddle")));
        // alyssa: 2 followers, bbitdiddle: 1, evaluator: 0

        List<String> influencers = lower(SocialNetwork.influencers(followsGraph));

        assertEquals("expected descending order of followers",
                Arrays.asList("alyssa", "bbitdiddle", "evaluator"), influencers);
    }

    // covers: > 1 users, some counts tied
    @Test
    public void testInfluencersTiedInfluence() {
        Map<String, Set<String>> followsGraph = new HashMap<>();
        followsGraph.put("evaluator", new HashSet<>(Arrays.asList("alyssa", "bbitdiddle")));
        // alyssa: 1 follower, bbitdiddle: 1 follower, evaluator: 0

        List<String> influencers = lower(SocialNetwork.influencers(followsGraph));

        // the spec does not say who comes first among tied users
        assertEquals("expected all three users", 3, influencers.size());
        assertTrue("alyssa must be in the first two places", influencers.subList(0, 2).contains("alyssa"));
        assertTrue("bbitdiddle must be in the first two places", influencers.subList(0, 2).contains("bbitdiddle"));
        assertEquals("evaluator (0 followers) must be last", "evaluator", influencers.get(2));
    }

    /*
     * Warning: all the tests you write here must be runnable against any
     * SocialNetwork class that follows the spec. It will be run against several
     * staff implementations of SocialNetwork, which will be done by overwriting
     * (temporarily) your version of SocialNetwork with the staff's version.
     * DO NOT strengthen the spec of SocialNetwork or its methods.
     * 
     * In particular, your test cases must not call helper methods of your own
     * that you have put in SocialNetwork, because that means you're testing a
     * stronger spec than SocialNetwork says. If you need such helper methods,
     * define them in a different class. If you only need them in this test
     * class, then keep them in this test class.
     */

    // ---------------- helpers (test class only) ----------------

    /*
     * Helper for the tests: true if follower follows followed in the graph,
     * ignoring case (the spec allows any case for usernames).
     */
    private static boolean follows(Map<String, Set<String>> followsGraph, String follower, String followed) {
        for (Map.Entry<String, Set<String>> entry : followsGraph.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(follower)) {
                for (String name : entry.getValue()) {
                    if (name.equalsIgnoreCase(followed)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /* Helper for the tests: the same list with every username in lowercase. */
    private static List<String> lower(List<String> names) {
        List<String> result = new ArrayList<>();
        for (String name : names) {
            result.add(name.toLowerCase());
        }
        return result;
    }

}