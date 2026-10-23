/* Extract.java **/
package twitter;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Extract {

    /**
     * Get the time period spanned by tweets.
     * @return a minimum-length time interval that contains the timestamp of
     *         every tweet in the list.
     */
    public static Timespan getTimespan(List<Tweet> tweets) {
        Instant earliest = tweets.get(0).getTimestamp();
        Instant latest = tweets.get(0).getTimestamp();
        for (Tweet tweet : tweets) {
            if (tweet.getTimestamp().isBefore(earliest)) {
                earliest = tweet.getTimestamp();
            }
            if (tweet.getTimestamp().isAfter(latest)) {
                latest = tweet.getTimestamp();
            }
        }
        return new Timespan(earliest, latest);
    }

    /**
     * Get usernames mentioned in a list of tweets.
     * @return the set of usernames who are mentioned in the text of the tweets.
     *         Twitter usernames are case-insensitive, and the returned set may
     *         include a username at most once.
     */
    public static Set<String> getMentionedUsers(List<Tweet> tweets) {
        Set<String> users = new HashSet<>();
        Pattern pattern = Pattern.compile("(?<![A-Za-z0-9_-])@([A-Za-z0-9_-]+)(?![A-Za-z0-9_-])");

        for (Tweet tweet : tweets) {
            Matcher matcher = pattern.matcher(tweet.getText());
            while (matcher.find()) {
                users.add(matcher.group(1).toLowerCase());
            }
        }
        return users;
    }

}
