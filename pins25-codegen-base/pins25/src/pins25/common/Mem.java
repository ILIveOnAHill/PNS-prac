package pins25.common;

import java.util.*;

/**
 * Klicni zapisi in dostopi do spremenljivk.
 * 
 * Vse spremenljivke (in parametri, ki so samo posebna vrsta spremenljivke)
 * katerih ime se zacne z {@code debug}, so lahko nastavljene na {@code null}.
 * Uporabljajo se samo za izpis sledenja delovanja abstraktnega skladovne
 * stroja.
 */
public class Mem {

	@SuppressWarnings({ "doclint:missing" })
	private Mem() {
		throw new Report.InternalError();
	}

	// --- KLICNI ZAPISI ---

	/**
	 * Klicni zapis.
	 */
	public static class Frame {

		/** Ime oznake, torej polno ime funkcije. */
		public final String name;

		/** Staticna globina funkcije. */
		public final Integer depth;

		/** Skupna velikost parametrov (skupaj s staticno povezavo). */
		public final Integer parsSize;

		/**
		 * Skupna velikost lokalnih spremenljivk (skupaj s shranjenim klicnim kazalcem
		 * in povratnim naslovom.
		 */
		public final Integer varsSize;

		/** Dostopi do parametrov. */
		public final List<RelAccess> debugPars;

		/** Dostopi do lokalnih spremenljivk. */
		public final List<RelAccess> debugVars;

		/**
		 * Ustvari nov klicni zapis.
		 * 
		 * @param name      Ime oznake, torej polno ime funkcije.
		 * @param depth     Staticna globina funkcije.
		 * @param parsSize  Skupna velikost parametrov (skupaj s staticno povezavo).
		 * @param varsSize  Skupna velikost lokalnih spremenljivk (skupaj s shranjenim
		 *                  klicnim kazalcem in povratnim naslovom.
		 * @param debugPars Dostopi do parametrov.
		 * @param debugVars Dostopi do lokalnih spremenljivk.
		 */
		public Frame(final String name, final Integer depth, final Integer parsSize, final Integer varsSize,
				List<RelAccess> debugPars, final List<RelAccess> debugVars) {
			this.name = name;
			this.depth = depth;
			this.parsSize = parsSize;
			this.varsSize = varsSize;
			this.debugPars = Collections.unmodifiableList(debugPars);
			this.debugVars = Collections.unmodifiableList(debugVars);
		}

	}

	// --- DOSTOPI DO SPREMENLJIVK ---

	/**
	 * Dostop do spremenljivke.
	 */
	public static abstract class Access {

		/** Velikost spremenljivke. */
		public final Integer size;

		/** Zacetna vrednost spremenljivke. */
		public final List<Integer> inits;

		/**
		 * Ustvari nov dostop do spremenljivke.
		 * 
		 * @param size  Velikost spremenljivke.
		 * @param inits Zacetna vrednost spremenljivke.
		 */
		public Access(final Integer size, final Vector<Integer> inits) {
			this.size = size;
			this.inits = inits == null ? null : Collections.unmodifiableList(new Vector<Integer>(inits));
		}

	}

	/**
	 * Absolutni dostop do spremenljivke (na staticen naslov).
	 */
	public static class AbsAccess extends Access {

		/** Ime oznake (ime spremenljivke). */
		public final String name;

		/**
		 * Ustvari nov absolutni dostop do spremenljivke.
		 * 
		 * @param name  Ime oznake (ime spremenljivke).
		 * @param size  Velikost spremenljivke.
		 * @param inits Zacetna vrednost spremenljivke.
		 */
		public AbsAccess(final String name, final Integer size, final Vector<Integer> inits) {
			super(size, inits);
			this.name = name;
		}

	}

	/**
	 * Relativni dostop do spremenljivke (na skladu).
	 */
	public static class RelAccess extends Access {

		/** Odmik od vrha klicnega zapisa, torej od vrednosti klicnega kazalca. */
		public final Integer offset;

		/** Staticna globina spremenljivke. */
		public final Integer depth;

		/** Ime spremenljivke. */
		public final String debugName;

		/**
		 * Ustvari nov relativni dostop do spremenljivke.
		 * 
		 * @param offset    Odmik od vrha klicnega zapisa, torej od vrednosti klicnega
		 *                  kazalca.
		 * @param depth     Staticna globina spremenljivke.
		 * @param size      Velikost spremenljivke.
		 * @param inits     Zacetna vrednost spremenljivke.
		 * @param debugName Ime spremenljivke.
		 */
		public RelAccess(final Integer offset, final Integer depth, Integer size, final Vector<Integer> inits,
				final String debugName) {
			super(size, inits);
			this.offset = offset;
			this.depth = depth;
			this.debugName = debugName;
		}

	}

}