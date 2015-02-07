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

  public List<TermInfo.Entry> parseDescription(String s) {
    List<TermInfo.Entry> entries = new ArrayList<>();
    parseDescription(s, 0, entries);
    return entries;
  }

  public int parseDescriptions(String s, int pos, List<TermInfo.Entry> entries) {
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

  public int parseDescription(String s, int pos, List<TermInfo.Entry> entries) {
    List<String> names = new ArrayList<>();
    pos = parseHeaderLine(s, pos, names);
    TermInfo.Entry entry = new TermInfo.Entry(names.get(0), names.subList(1, names.size()));
    List<TermInfo.Feature> features = new ArrayList<>();
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

  public static int parseFeatureLines(String s, int pos, List<TermInfo.Feature> features) {
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

  public static int parseFeatureLine(String s, int pos, List<TermInfo.Feature> features) {
    if (pos < s.length()) {
      char first = s.charAt(pos);
      if (first == ' ' || first == '\t') {
        int to = s.indexOf(",\n", ++pos);
        if (to == -1) {
          throw new IllegalArgumentException();
        }
        while (true) {
          int next = s.indexOf(',', pos);
          if (next > to) {
            next = -1;
          }
          if (next == -1) {
            return to + 2;
          } else {
            // Tolerate empty features
            if (next > pos) {
              Matcher matcher = FEATURE_PATTERN.matcher(s).region(pos, next);
              if (!matcher.find()) {
                throw new IllegalArgumentException("Invalid feature : >" + s.substring(pos, next) + "<");
              }
              if (matcher.group(1) != null) {
                features.add(new TermInfo.Feature.String(matcher.group(1), matcher.group(2)));
              } else if (matcher.group(3) != null) {
                features.add(new TermInfo.Feature.Numeric(matcher.group(3), matcher.group(4)));
              } else if (matcher.group(5) != null) {
                features.add(new TermInfo.Feature.Boolean(matcher.group(5)));
              }
            }
            pos = next + 1;
          }
        }
      }
    }
    return pos;
  }

  public static int parseFully(Pattern pattern, String s, int pos) {
    Matcher matcher = pattern.matcher(s).region(pos, s.length()).useAnchoringBounds(true);
    if (!matcher.find()) {
      String snippet = s.substring(Math.max(0, pos - 10), Math.min(s.length(), pos + 100));
      throw new IllegalArgumentException("Bug at " + pos + " : <" + snippet + ">");
    }
    return matcher.end();
  }
}
