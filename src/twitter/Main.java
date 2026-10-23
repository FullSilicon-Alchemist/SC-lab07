/* Copyright (c) 2007-2016 MIT 6.005 course staff, all rights reserved.
 * Redistribution of original or derived work requires permission of course staff.
 */
package twitter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * This is the main program.
 * 
 * You may change this class if you wish, but you don't have to.
 * 
 * The MIT tweet server is no longer online, so this version reads sample
 * tweets from the file tweets.txt in the project folder. Each line has the
 * form: id|author|timestamp|text
 */
public class Main {

    /** File with the sample tweets, in the project folder (next to src and test). */
    public static final String TWEETS_FILE = "tweets.txt";

    /** Read tweets from a file with lines of the form id|author|timestamp|text. */
    private static List<Tweet> readTweetsFromFile(String fileName) throws IOException {
        List<Tweet> tweets = new ArrayList<>();
        for (String line : Files.readAllLines(Paths.get(fileName), StandardCharsets.UTF_8)) {
            if (line.trim().isEmpty()) {
                continue;
            }
            String[] parts = line.split("\\|", 4);
            tweets.add(new Tweet(Long.parseLong(parts[0].trim()), parts[1].trim(),
                    parts[3], Instant.parse(parts[2].trim())));
        }
        return tweets;
    }

    /**
     * Main method of the program. Reads a sample of tweets and prints some
     * facts about it.
     * 
     * @param args command-line arguments (not used)
     */
    public static void main(String[] args) {
        final List<Tweet> tweets;
        try {
            tweets = readTweetsFromFile(TWEETS_FILE);
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }

        // display some characteristics about the tweets
        System.out.println("read " + tweets.size() + " tweets");

        final Timespan span = Extract.getTimespan(tweets);
        System.out.println("ranging from " + span.getStart() + " to " + span.getEnd());

        final Set<String> mentionedUsers = Extract.getMentionedUsers(tweets);
        System.out.println("covers " + mentionedUsers.size() + " Twitter users");

        // infer the follows graph
        final Map<String, Set<String>> followsGraph = SocialNetwork.guessFollowsGraph(tweets);
        System.out.println("follows graph has " + followsGraph.size() + " nodes");

        // number of followers of every user, only used for printing
        final Map<String, Integer> followerCount = new HashMap<>();
        for (Set<String> followed : followsGraph.values()) {
            for (String name : followed) {
                followerCount.merge(name.toLowerCase(Locale.ROOT), 1, Integer::sum);
            }
        }

        // print the top-N influencers
        final int count = 10;
        final List<String> influencers = SocialNetwork.influencers(followsGraph);
        System.out.println("top influencers:");
        for (String username : influencers.subList(0, Math.min(count, influencers.size()))) {
            System.out.println(username + " - " + followerCount.getOrDefault(username.toLowerCase(Locale.ROOT), 0) + " followers");
        }
    }

}