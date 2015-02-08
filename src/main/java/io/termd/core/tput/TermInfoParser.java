package io.termd.core.tput;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class TermInfoParser {

  public static final Pattern BLANK_OR_COMMENT = Pattern.compile("^[ \\t]*(?:#.*)?(?:\\n|$)");
  private static final String ALIAS = "([\\x21-\\x7E&&[^,/|]]+)";
  private static final Pattern ALIAS_PATTERN = Pattern.compile("^" + ALIAS + "(?=\\|)");
  private static final String LONGNAME = "([\\x20-\\x7E&&[^|]]+)";
  private static final Pattern LONGNAME_PATTERN = Pattern.compile("^" + LONGNAME + ",\\n");

  public List<TermInfoEntry> parseDescription(String s) {
    List<TermInfoEntry> entries = new ArrayList<>();
    parseDescription(s, 0, entries);
    return entries;
  }

  public int parseDescriptions(String s, int pos, List<TermInfoEntry> entries) {
    while (pos < s.length()) {
      Matcher matcher = BLANK_OR_COMMENT.matcher(s).region(pos, s.length()).useAnchoringBounds(true).useTransparentBounds(true);
      if (matcher.find()) {
        pos = matcher.end();
        continue;
      }
      pos = parseDescription(s, pos, entries);
    }
    return pos;
  }

  public int parseDescription(String s, int pos, List<TermInfoEntry> entries) {
    List<String> names = new ArrayList<>();
    pos = parseHeaderLine(s, pos, names);
    TermInfoEntry entry = new TermInfoEntry(names.get(0), names.subList(1, names.size()));
    List<TermInfoFeature> features = new ArrayList<>();
    pos = parseFeatureLines(s, pos, features);
    entry.features.addAll(features);
    entries.add(entry);
    return pos;
  }

  public int parseHeaderLine(String s, int pos, List<String> names) {
    while (true) {
      try {
        int end = parseAlias(s, pos);
        String alias = s.substring(pos, end);
        names.add(alias);
        pos = end + 1;
      } catch (IllegalArgumentException ignore) {
        int end = parseLongName(s, pos);
        String name = s.substring(pos, end - 2);
        names.add(name);
        pos = end;
        break;
      }
    }
    return pos;
  }

  public static int parseAlias(String s, int pos) {
    return parseFully(ALIAS_PATTERN, s, pos);
  }

  public static int parseLongName(String s, int pos) {
    return parseFully(LONGNAME_PATTERN, s, pos);
  }

  public static int parseFeatureLines(String s, int pos, List<TermInfoFeature> features) {
    int next = parseFeatureLine(s, pos, features);
    if (next == pos) {
      throw new IllegalArgumentException();
    }
    pos = next;
    while (true) {
      next = parseFeatureLine(s, pos, features);
      if (next == pos) {
        break;
      }
      pos = next;
    }
    return pos;
  }

  private static final String STRING  = "(?:([\\x20-\\x7E&&[^,=#]]+)=([\\x20-\\x7E&&[^,]]+))";
  private static final String NUMERIC = "([\\x20-\\x7E&&[^,=#]]+)#([0-9]+)";
  private static final String BOOLEAN = "([\\x20-\\x7E&&[^,=#]]+)";
  private static final Pattern FEATURE_PATTERN = Pattern.compile(STRING + "|" + NUMERIC + "|" + BOOLEAN);
  private static final Pattern FEATURE_END_PATTERN = Pattern.compile(",[ \\t]*\\n");
  private static final Pattern COMMA_PATTERN = Pattern.compile(",[ \\t]*");

  public static int parseFeatureLine(String s, int pos, List<TermInfoFeature> features) {
    if (pos < s.length()) {
      char first = s.charAt(pos);
      if (first == ' ' || first == '\t') {
        Matcher featureEndMatcher = FEATURE_END_PATTERN.matcher(s).region(++pos, s.length());
        if (!featureEndMatcher.find()) {
          throw new IllegalArgumentException();
        }
        Matcher commaMatcher = COMMA_PATTERN.matcher(s).region(pos, featureEndMatcher.start() + 1);
        while (true) {
          if (!commaMatcher.find()) {
            return featureEndMatcher.end();
          } else {
            // Tolerate empty features
            int next = commaMatcher.start();
            if (next > pos) {
              Matcher featureMatcher = FEATURE_PATTERN.matcher(s).region(pos, next);
              if (!featureMatcher.find()) {
                throw new IllegalArgumentException();
              }
              if (featureMatcher.group(1) != null) {
                features.add(new TermInfoFeature.String(featureMatcher.group(1), featureMatcher.group(2)));
              } else if (featureMatcher.group(3) != null) {
                features.add(new TermInfoFeature.Numeric(featureMatcher.group(3), featureMatcher.group(4)));
              } else if (featureMatcher.group(5) != null) {
                features.add(new TermInfoFeature.Boolean(featureMatcher.group(5)));
              }
            }
            pos = commaMatcher.end();
          }
        }
      }
    }
    return pos;
  }

  public static int parseFully(Pattern pattern, String s, int pos) {
    Matcher matcher = pattern.matcher(s).region(pos, s.length()).useAnchoringBounds(true);
    if (!matcher.find()) {
      throw new IllegalArgumentException();
    }
    return matcher.end();
  }
}
