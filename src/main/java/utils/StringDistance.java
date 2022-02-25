package utils;

import java.io.IOException;
import java.util.List;

import beans.CabComuni;
import beans.CabComuniMatch;



public class StringDistance {

	public static CabComuniMatch minDistanceCabComuni(List<CabComuni> masterList, String toMatch) {
		
		int absoluteBest = 1000;
		CabComuni masterBest = null;
		
		for (CabComuni master : masterList) {
			
			int best = distance(master.getComune(), toMatch);
			//System.out.println("DISTANCE: "+ toMatch +"-" +master.getComune()+" = "+best);
			if(best<absoluteBest) {
				masterBest = master;
				absoluteBest = best;
			}
			
		}
		
		return new CabComuniMatch(masterBest, absoluteBest);
	}
	
	public static String minDistance(List<String> masterList, String toMatch) {
		
		int absoluteBest = -1;
		String masterBest = null;
		
		for (String master : masterList) {
			
			int best = distance(master, toMatch);
			
			if(best<absoluteBest) {
				masterBest = master;
				absoluteBest = best;
			}
			
		}
		return masterBest;
	}
	
	public static int distance(String master, String toMatch) {
		
		GeneralEditDistance ed = GeneralEditDistances.getLevenshteinDistance(master);
		
	    int best = Integer.MAX_VALUE;
	    for (int i = 0; i < 80*200; i++) {
	        best = Math.min(best, ed.getDistance(toMatch, best));
	    }
		
	    return best;
	}
	
	public static void main(String[] args) throws IOException {

	    String line1 = "ABBIATEGRASSO";
	    String line2 = "ABBIATGRASSO";
	    GeneralEditDistance ed = GeneralEditDistances.getLevenshteinDistance(line1);

	    long time = System.currentTimeMillis();
	    int best = Integer.MAX_VALUE;
	    for (int i = 0; i < 80*200; i++) {
	        best = Math.min(best, ed.getDistance(line2, best));
	    }
	    System.out.println("Best distance = " + best + " Total time (ms): " + (System.currentTimeMillis() - time));
	}}

	abstract class CharIndex {

	static class FullHash extends CharIndex {

	    static class Char {
	        char c;

	        @Override
	        public boolean equals(Object x) {
	            return (x != null) && (((Char) x).c == this.c);
	        }

	        @Override
	        public int hashCode() {
	            return c;
	        }

	        @Override
	        public String toString() {
	            return "'" + c + "'";
	        }
	    }

	    static final int NULL_ELEMENT = 0;
	    protected int lastUsed = NULL_ELEMENT;
	    final java.util.HashMap<Char, Integer> map;

	    FullHash(CharSequence s) {
	        int len = s.length();
	        int power = Integer.highestOneBit(len);

	        map = new java.util.HashMap<Char, Integer>(
	                power << ((power == len) ? 1 : 2));

	        Char test = new Char();                   /* (re)used for lookup */
	        for (int i = 0; i < s.length(); i++) {
	            test.c = s.charAt(i);
	            if (map.get(test) == null) {
	                map.put(test, new Integer(++lastUsed));
	                test = new Char();
	            }
	        }
	    }

	    @Override
	    public int lookup(char c) {
	        final Char lookupTest = new Char();
	        lookupTest.c = c;
	        Integer result = map.get(lookupTest);
	        return (result != null) ? result.intValue() : 0;
	    }

	    @Override
	    public int[] map(CharSequence s, int[] mapped) {
	        /* Create one mutable Char, and reuse it for all lookups */
	        final Char lookupTest = new Char();

	        int len = s.length();
	        if (mapped.length < len) {
	            mapped = new int[len];
	        }

	        for (int i = 0; i < len; i++) {
	            lookupTest.c = s.charAt(i);
	            Integer result = map.get(lookupTest);
	            mapped[i] = (result != null) ? result.intValue() : NULL_ELEMENT;
	        }
	        return mapped;
	    }

	    @Override
	    public int nullElement() {
	        return NULL_ELEMENT;
	    }

	    @Override
	    public int size() {
	        return lastUsed + 1;
	    }
	}

	static class Masked extends CharIndex {
	    static final int SIZE = 0x100;
	    static final int MASK = (SIZE - 1);
	    static final int NULL_ELEMENT = SIZE;

	    static Masked generate(CharSequence s) {
	        char[] contains = new char[SIZE];
	        contains[0] = (char) 1;
	        for (int i = 0; i < s.length(); i++) {
	            char c = s.charAt(i);
	            int index = c & MASK;
	            if (contains[index] != c) {
	                if ((contains[index] & MASK) == index) {
	                    return null;
	                }
	                contains[index] = c;
	            }
	        }
	        return new Masked(contains);
	    }

	    final char[] contains;

	    private Masked(char[] contains) {
	        this.contains = contains;
	    }

	    @Override
	    public int lookup(char c) {
	        int index = c & MASK;
	        return (c == contains[index]) ? index : NULL_ELEMENT;
	    }

	    @Override
	    public int[] map(CharSequence s, int[] mapped) {
	        int len = s.length();
	        if (mapped.length < len) {
	            mapped = new int[len];
	        }
	        for (int i = 0; i < len; i++) {
	            char c = s.charAt(i);
	            int index = c & MASK;
	            mapped[i] = (c == contains[index]) ? index : NULL_ELEMENT;
	        }
	        return mapped;
	    }

	    @Override
	    public int nullElement() {
	        return NULL_ELEMENT;
	    }

	    @Override
	    public int size() {
	        return NULL_ELEMENT + 1;
	    }
	}

	static class Straight extends CharIndex {
	    static final int MAX = 0x80;
	    static final int MASK = ~(MAX - 1);
	    static final int NULL_ELEMENT = MAX;

	    static Straight generate(CharSequence s) {
	        for (int i = 0; i < s.length(); i++) {
	            if ((s.charAt(i) & MASK) != 0) {
	                return null;
	            }
	        }

	        return new Straight();
	    }

	    private Straight() {
	    }

	    @Override
	    public int lookup(char c) {
	        return ((c & MASK) == 0) ? c : NULL_ELEMENT;
	    }

	    @Override
	    public int[] map(CharSequence s, int[] mapped) {
	        int len = s.length();
	        if (mapped.length < len) {
	            mapped = new int[len];
	        }
	        for (int i = 0; i < len; i++) {
	            char c = s.charAt(i);
	            mapped[i] = ((c & MASK) == 0) ? c : NULL_ELEMENT;
	        }
	        return mapped;
	    }

	    @Override
	    public int nullElement() {
	        return NULL_ELEMENT;
	    }

	    @Override
	    public int size() {
	        return NULL_ELEMENT + 1;
	    }
	}

	public static CharIndex getInstance(CharSequence s) {
	    CharIndex result;

	    if ((result = Straight.generate(s)) != null) {
	        return result;
	    }

	    if ((result = Masked.generate(s)) != null) {
	        return result;
	    }

	    return new FullHash(s);
	}

	public abstract int lookup(char c);

	public abstract int[] map(CharSequence s, int[] mapped);

	public abstract int nullElement();

	public abstract int size();
	}


	interface GeneralEditDistance {

	GeneralEditDistance duplicate();

	int getDistance(CharSequence target, int limit);

	}


	class GeneralEditDistances {

	private static class Levenshtein implements GeneralEditDistance {
	    private ModifiedBerghelRoachEditDistance berghel;
	    private MyersBitParallelEditDistance myers;
	    private final CharSequence pattern;
	    private final int patternLength;

	    private Levenshtein(CharSequence pattern) {
	        this.pattern = pattern;
	        this.patternLength = pattern.length();
	    }

	    public GeneralEditDistance duplicate() {
	        Levenshtein dup = new Levenshtein(pattern);
	        if (this.myers != null) {
	            dup.myers = (MyersBitParallelEditDistance) this.myers.duplicate();
	        }
	        return dup;
	    }

	    public int getDistance(CharSequence target, int limit) {
	        if (limit <= 1) {
	            return limit == 0 ?
	                    (pattern.equals(target) ? 0 : 1) :
	                    atMostOneError(pattern, target);
	        }
	        if ((patternLength > 64)
	                && (limit < (target.length() / 10))) {
	            if (berghel == null) {
	                berghel = ModifiedBerghelRoachEditDistance.getInstance(pattern);
	            }
	            return berghel.getDistance(target, limit);
	        }

	        if (myers == null) {
	            myers = MyersBitParallelEditDistance.getInstance(pattern);
	        }

	        return myers.getDistance(target, limit);
	    }
	}

	public static int atMostOneError(CharSequence s1, CharSequence s2) {
	    int s1Length = s1.length();
	    int s2Length = s2.length();
	    int errors = 0;             /* running count of edits required */

	    switch (s2Length - s1Length) {
	        /*
	        * Strings are the same length.  No single insert/delete is possible;
	        * at most one substitution can be present.
	        */
	        case 0:
	            for (int i = 0; i < s2Length; i++) {
	                if ((s2.charAt(i) != s1.charAt(i)) && (errors++ != 0)) {
	                    break;
	                }
	            }
	            return errors;

	        case 1: /* s2Length > s1Length */
	            for (int i = 0; i < s1Length; i++) {
	                if (s2.charAt(i) != s1.charAt(i)) {
	                    for (; i < s1Length; i++) {
	                        if (s2.charAt(i + 1) != s1.charAt(i)) {
	                            return 2;
	                        }
	                    }
	                    return 1;
	                }
	            }
	            return 1;

	        /* Same as above case, with strings reversed */
	        case -1: /* s1Length > s2Length */
	            for (int i = 0; i < s2Length; i++) {
	                if (s2.charAt(i) != s1.charAt(i)) {
	                    for (; i < s2Length; i++) {
	                        if (s2.charAt(i) != s1.charAt(i + 1)) {
	                            return 2;
	                        }
	                    }
	                    return 1;
	                }
	            }
	            return 1;

	        /* Edit distance is at least difference in lengths; more than 1 here. */
	        default:
	            return 2;
	    }
	}

	public static GeneralEditDistance
	getLevenshteinDistance(CharSequence pattern) {
	    return new Levenshtein(pattern);
	}

	private GeneralEditDistances() {
	}
	}

	class ModifiedBerghelRoachEditDistance implements GeneralEditDistance {

	private static final int[] EMPTY_INT_ARRAY = new int[0];

	public static ModifiedBerghelRoachEditDistance
	getInstance(CharSequence pattern) {
	    return getInstance(pattern.toString());
	}

	public static ModifiedBerghelRoachEditDistance
	getInstance(String pattern) {
	    return new ModifiedBerghelRoachEditDistance(pattern.toCharArray());
	}

	private int[] currentLeft = EMPTY_INT_ARRAY;

	private int[] currentRight = EMPTY_INT_ARRAY;

	private int[] lastLeft = EMPTY_INT_ARRAY;

	private int[] lastRight = EMPTY_INT_ARRAY;

	private final char[] pattern;

	private int[] priorLeft = EMPTY_INT_ARRAY;

	private int[] priorRight = EMPTY_INT_ARRAY;

	private ModifiedBerghelRoachEditDistance(char[] pattern) {
	    this.pattern = pattern;
	}

	public ModifiedBerghelRoachEditDistance duplicate() {
	    return new ModifiedBerghelRoachEditDistance(pattern);
	}

	public int getDistance(CharSequence targetSequence, int limit) {
	    final int targetLength = targetSequence.length();

	    final int main = pattern.length - targetLength;
	    int distance = Math.abs(main);
	    if (distance > limit) {
	        /* More than we wanted.  Give up right away */
	        return Integer.MAX_VALUE;
	    }

	    final char[] target = new char[targetLength];
	    for (int i = 0; i < targetLength; i++) {
	        target[i] = targetSequence.charAt(i);
	    }
	    if (main <= 0) {
	        ensureCapacityRight(distance, false);
	        for (int j = 0; j <= distance; j++) {
	            lastRight[j] = distance - j - 1;  /* Make diagonal -k start in row k */
	            priorRight[j] = -1;
	        }
	    } else {
	        ensureCapacityLeft(distance, false);
	        for (int j = 0; j <= distance; j++) {
	            lastLeft[j] = -1;                 /* Make diagonal +k start in row 0 */
	            priorLeft[j] = -1;
	        }
	    }

	    boolean even = true;

	    while (true) {

	        int offDiagonal = (distance - main) / 2;
	        ensureCapacityRight(offDiagonal, true);

	        if (even) {
	            lastRight[offDiagonal] = -1;
	        }

	        int immediateRight = -1;
	        for (; offDiagonal > 0; offDiagonal--) {
	            currentRight[offDiagonal] = immediateRight = computeRow(
	                    (main + offDiagonal),
	                    (distance - offDiagonal),
	                    pattern,
	                    target,
	                    priorRight[offDiagonal - 1],
	                    lastRight[offDiagonal],
	                    immediateRight);
	        }

	        offDiagonal = (distance + main) / 2;
	        ensureCapacityLeft(offDiagonal, true);

	        if (even) {
	            lastLeft[offDiagonal] = (distance - main) / 2 - 1;
	        }

	        int immediateLeft = even ? -1 : (distance - main) / 2;

	        for (; offDiagonal > 0; offDiagonal--) {
	            currentLeft[offDiagonal] = immediateLeft = computeRow(
	                    (main - offDiagonal),
	                    (distance - offDiagonal),
	                    pattern, target,
	                    immediateLeft,
	                    lastLeft[offDiagonal],
	                    priorLeft[offDiagonal - 1]);
	        }

	        int mainRow = computeRow(main, distance, pattern, target,
	                immediateLeft, lastLeft[0], immediateRight);

	        if ((mainRow == targetLength) || (++distance > limit) || (distance < 0)) {
	            break;
	        }

	        /* The [0] element goes to both sides. */
	        currentLeft[0] = currentRight[0] = mainRow;

	        /* Rotate rows around for next round: current=>last=>prior (=>current) */
	        int[] tmp = priorLeft;
	        priorLeft = lastLeft;
	        lastLeft = currentLeft;
	        currentLeft = priorLeft;

	        tmp = priorRight;
	        priorRight = lastRight;
	        lastRight = currentRight;
	        currentRight = tmp;

	        /* Update evenness, too */
	        even = !even;
	    }

	    return distance;
	}

	private int computeRow(int k, int p, char[] a, char[] b,
	                       int knownLeft, int knownAbove, int knownRight) {
	    assert (Math.abs(k) <= p);
	    assert (p >= 0);

	    int t;
	    if (p == 0) {
	        t = 0;
	    } else {
	        t = Math.max(Math.max(knownAbove, knownRight) + 1, knownLeft);
	    }

	    int tmax = Math.min(b.length, (a.length - k));

	    while ((t < tmax) && b[t] == a[t + k]) {
	        t++;
	    }

	    return t;
	}

	private void ensureCapacityLeft(int index, boolean copy) {
	    if (currentLeft.length <= index) {
	        index++;
	        priorLeft = resize(priorLeft, index, copy);
	        lastLeft = resize(lastLeft, index, copy);
	        currentLeft = resize(currentLeft, index, false);
	    }
	}

	private void ensureCapacityRight(int index, boolean copy) {
	    if (currentRight.length <= index) {
	        index++;
	        priorRight = resize(priorRight, index, copy);
	        lastRight = resize(lastRight, index, copy);
	        currentRight = resize(currentRight, index, false);
	    }
	}

	private int[] resize(int[] array, int size, boolean copy) {
	    int[] result = new int[size];
	    if (copy) {
	        System.arraycopy(array, 0, result, 0, array.length);
	    }
	    return result;
	}
	}

	abstract class MyersBitParallelEditDistance
	    implements GeneralEditDistance, Cloneable {

	static class Empty extends MyersBitParallelEditDistance {
	    Empty(CharSequence s) {
	        super(s);
	    }

	    @Override
	    public GeneralEditDistance duplicate() {
	        return this;      /* thread-safe */
	    }

	    @Override
	    public int getDistance(CharSequence s, int k) {
	        return s.length();
	    }
	}

	static class Multi extends MyersBitParallelEditDistance {
	    int count;
	    final int lastBitPosition;
	    final int[][] positions;
	    int[] verticalNegativesReusable;
	    int[] verticalPositivesReusable;
	    final int wordMask = (-1 >>> 1);
	    final int wordSize = Integer.SIZE - 1;

	    Multi(CharSequence s) {
	        super(s);
	        count = (m + wordSize - 1) / wordSize;
	        positions = PatternBitmap.map(s, idx, new int[idx.size()][], wordSize);
	        lastBitPosition = (1 << ((m - 1) % wordSize));
	        perThreadInit();
	    }

	    @Override
	    public int getDistance(CharSequence s, int k) {
	        indices = idx.map(s, indices);

	        int[] verticalPositives = verticalPositivesReusable;
	        java.util.Arrays.fill(verticalPositives, wordMask);
	        int[] verticalNegatives = verticalNegativesReusable;
	        java.util.Arrays.fill(verticalNegatives, 0);

	        int distance = m;
	        int len = s.length();

	        int maxMisses = k + len - m;
	        if (maxMisses < 0) {
	            maxMisses = Integer.MAX_VALUE;
	        }

	        outer:
	        for (int j = 0; j < len; j++) {
	            int[] position = positions[indices[j]];

	            int sum = 0;
	            int horizontalPositiveShift = 1;
	            int horizontalNegativeShift = 0;

	            for (int i = 0; i < count; i++) {
	                int verticalNegative = verticalNegatives[i];
	                int patternMatch = (position[i] | verticalNegative);
	                int verticalPositive = verticalPositives[i];
	                sum = (verticalPositive & patternMatch)
	                        + (verticalPositive) + (sum >>> wordSize);
	                int diagonalZero = ((sum & wordMask) ^ verticalPositive)
	                        | patternMatch;
	                int horizontalPositive = (verticalNegative
	                        | ~(diagonalZero | verticalPositive));
	                int horizontalNegative = diagonalZero & verticalPositive;

	                if (i == (count - 1)) {            /* only last bit in last word */
	                    if ((horizontalNegative & lastBitPosition) != 0) {
	                        distance--;
	                    } else if ((horizontalPositive & lastBitPosition) != 0) {
	                        distance++;
	                        if ((maxMisses -= 2) < 0) {
	                            break outer;
	                        }
	                    } else if (--maxMisses < 0) {
	                        break outer;
	                    }
	                }

	                horizontalPositive = ((horizontalPositive << 1)
	                        | horizontalPositiveShift);
	                horizontalPositiveShift = (horizontalPositive >>> wordSize);

	                horizontalNegative = ((horizontalNegative << 1)
	                        | horizontalNegativeShift);
	                horizontalNegativeShift = (horizontalNegative >>> wordSize);

	                verticalPositives[i] = (horizontalNegative
	                        | ~(diagonalZero | horizontalPositive))
	                        & wordMask;
	                verticalNegatives[i] = (diagonalZero & horizontalPositive) & wordMask;
	            }
	        }
	        return distance;
	    }

	    @Override
	    protected void perThreadInit() {
	        super.perThreadInit();
	        verticalPositivesReusable = new int[count];
	        verticalNegativesReusable = new int[count];
	    }
	}

	static class TYPEint/*WORD*/ extends MyersBitParallelEditDistance {
	    final int/*WORD*/ lastBitPosition;
	    final int/*WORD*/[] map;

	    @SuppressWarnings("cast")
	    TYPEint/*WORD*/(CharSequence s) {
	        super(s);
	        /* Precompute bitmaps for this pattern */
	        map = PatternBitmap.map(s, idx, new int/*WORD*/[idx.size()]);
	        /* Compute the bit that represents a change in the last row */
	        lastBitPosition = (((int/*WORD*/) 1) << (m - 1));
	    }

	    @Override
	    public int getDistance(CharSequence s, int k) {
	        int len = s.length();

	        /* Quick check based on length */
	        if (((len - m) > k) || ((m - len) > k)) {
	            return k + 1;
	        }

	        /* Map characters to their integer positions in the bitmap array */
	        indices = idx.map(s, indices);

	        /* Initially, vertical change is all positive (none negative) */
	        int/*WORD*/ verticalPositive = -1;
	        int/*WORD*/ verticalNegative = 0;
	        int distance = m;

	        /* We can only miss the "distance--" below this many times: */
	        int maxMisses = k + len - m;
	        if (maxMisses < 0) {
	            maxMisses = Integer.MAX_VALUE;
	        }

	        for (int j = 0; j < len; j++) {
	            /* Where is diagonal zero: matches, or prior VN; plus recursion */
	            int/*WORD*/ diagonalZero = map[indices[j]] | verticalNegative;
	            diagonalZero |= (((diagonalZero & verticalPositive) + verticalPositive)
	                    ^ verticalPositive);

	            /* Compute horizontal changes */
	            int/*WORD*/ horizontalPositive = verticalNegative
	                    | ~(diagonalZero | verticalPositive);
	            int/*WORD*/ horizontalNegative = diagonalZero & verticalPositive;

	            /* Update final distance based on horizontal changes */
	            if ((horizontalNegative & lastBitPosition) != 0) {
	                distance--;
	            } else if ((horizontalPositive & lastBitPosition) != 0) {
	                distance++;
	                if ((maxMisses -= 2) < 0) {
	                    break;
	                }
	            } else if (--maxMisses < 0) {
	                break;
	            }

	            /* Shift Hs to next row, compute new Vs analagously to Hs above */
	            horizontalPositive = (horizontalPositive << 1) | 1;
	            verticalPositive = (horizontalNegative << 1)
	                    | ~(diagonalZero | horizontalPositive);
	            verticalNegative = diagonalZero & horizontalPositive;
	        }
	        return distance;
	    }
	}

	static class TYPElong/*WORD*/ extends MyersBitParallelEditDistance {
	    final long/*WORD*/ lastBitPosition;
	    final long/*WORD*/[] map;

	    TYPElong/*WORD*/(CharSequence s) {
	        super(s);
	        /* Precompute bitmaps for this pattern */
	        map = PatternBitmap.map(s, idx, new long/*WORD*/[idx.size()]);
	        /* Compute the bit that represents a change in the last row */
	        lastBitPosition = (((long/*WORD*/) 1) << (m - 1));
	    }

	    @Override
	    public int getDistance(CharSequence s, int k) {
	        int len = s.length();

	        /* Quick check based on length */
	        if (((len - m) > k) || ((m - len) > k)) {
	            return k + 1;
	        }

	        /* Map characters to their integer positions in the bitmap array */
	        indices = idx.map(s, indices);

	        /* Initially, vertical change is all positive (none negative) */
	        long/*WORD*/ verticalPositive = -1;
	        long/*WORD*/ verticalNegative = 0;
	        int distance = m;

	        /* We can only miss the "distance--" below this many times: */
	        int maxMisses = k + len - m;
	        if (maxMisses < 0) {
	            maxMisses = Integer.MAX_VALUE;
	        }

	        for (int j = 0; j < len; j++) {
	            /* Where is diagonal zero: matches, or prior VN; plus recursion */
	            long/*WORD*/ diagonalZero = map[indices[j]] | verticalNegative;
	            diagonalZero |= (((diagonalZero & verticalPositive) + verticalPositive)
	                    ^ verticalPositive);

	            /* Compute horizontal changes */
	            long/*WORD*/ horizontalPositive = verticalNegative
	                    | ~(diagonalZero | verticalPositive);
	            long/*WORD*/ horizontalNegative = diagonalZero & verticalPositive;

	            /* Update final distance based on horizontal changes */
	            if ((horizontalNegative & lastBitPosition) != 0) {
	                distance--;
	            } else if ((horizontalPositive & lastBitPosition) != 0) {
	                distance++;
	                if ((maxMisses -= 2) < 0) {
	                    break;
	                }
	            } else if (--maxMisses < 0) {
	                break;
	            }

	            /* Shift Hs to next row, compute new Vs analagously to Hs above */
	            horizontalPositive = (horizontalPositive << 1) | 1;
	            verticalPositive = (horizontalNegative << 1)
	                    | ~(diagonalZero | horizontalPositive);
	            verticalNegative = diagonalZero & horizontalPositive;
	        }
	        return distance;
	    }
	}

	public static MyersBitParallelEditDistance getInstance(CharSequence s) {
	    int m = s.length();
	    return (m <= Integer.SIZE) ?
	            ((m == 0) ? new Empty(s) : new TYPEint(s)) :
	            (s.length() <= Long.SIZE) ?
	                    new TYPElong(s) :
	                    new Multi(s);
	}

	final CharIndex idx;

	int[] indices = new int[0];

	final int m;

	protected MyersBitParallelEditDistance(CharSequence s) {
	    m = s.length();
	    idx = CharIndex.getInstance(s);
	}

	public GeneralEditDistance duplicate() {
	    try {
	        return (MyersBitParallelEditDistance) clone();
	    } catch (CloneNotSupportedException x) { /*IMPOSSIBLE */
	        throw new IllegalStateException("Cloneable object would not clone");
	    }
	}

	public abstract int getDistance(CharSequence s, int k);

	@Override
	protected Object clone() throws CloneNotSupportedException {
	    Object obj = super.clone();

	    /* Re-initialize any non-thread-safe parts */
	    ((MyersBitParallelEditDistance) obj).perThreadInit();

	    return obj;
	}

	protected void perThreadInit() {
	    indices = new int[0];
	}
	}

	class PatternBitmap {

	public static int[] map(CharSequence s, CharIndex idx, int[] result) {
	    int len = s.length();
	    assert (len <= Integer.SIZE);
	    for (int i = 0; i < len; i++) {
	        result[idx.lookup(s.charAt(i))] |= (1 << i);
	    }
	    return result;
	}

	public static int[][] map(CharSequence s, CharIndex idx,
	                          int[][] result, int width) {
	    assert (width <= Integer.SIZE);
	    int len = s.length();
	    int rowSize = (len + width - 1) / width;

	    /*
	    * Use one zero-filled bitmap for alphabet characters not in the pattern
	    */
	    int[] nullElement = new int[rowSize];
	    java.util.Arrays.fill(result, nullElement);

	    int wordIndex = 0;          /* Which word we are on now */
	    int bitWithinWord = 0;      /* Which bit within that word */

	    for (int i = 0; i < s.length(); i++) {
	        int[] r = result[idx.lookup(s.charAt(i))];
	        if (r == nullElement) {

	            /* Create a separate zero-filled bitmap for this alphabet character */
	            r = result[idx.lookup(s.charAt(i))] = new int[rowSize];
	        }
	        r[wordIndex] |= (1 << bitWithinWord);

	        /* Step to the next bit (and word if appropriate) */
	        if (++bitWithinWord == width) {
	            bitWithinWord = 0;
	            wordIndex++;
	        }
	    }
	    return result;
	}

	public static long[] map(CharSequence s, CharIndex idx, long[] result) {
	    int len = s.length();
	    assert (len <= Long.SIZE);
	    for (int i = 0; i < len; i++) {
	        result[idx.lookup(s.charAt(i))] |= (1L << i);
	    }
	    return result;
	}

	private PatternBitmap() { /* Prevent instantiation */ }
	}
