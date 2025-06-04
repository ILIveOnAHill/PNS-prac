package pins25.common;

/**
 * Ukazi skladovnega stroja.
 * 
 * Vse spremenljivke (in parametri, ki so samo posebna vrsta spremenljivke),
 * katerih ime se zacne z {@code debug}, so lahko nastavljene na {@code null}.
 * Uporabljajo se samo za izpis sledenja delovanja skladovnega stroja.
 */
public class PDM {

	@SuppressWarnings({ "doclint:missing" })
	public PDM() {
		throw new Report.InternalError();
	}

	/**
	 * Vrsta ukazov skladovnega stroja.
	 */
	public static interface Instruction {

		/**
		 * Vrne dolzino ukaza.
		 * 
		 * @return Dolzina ukaza.
		 */
		Integer size();

	}

	/** Ukazi skladovnega stroja za opis podatkov. */
	public static interface DataInstr extends Instruction {
	}

	/** Ukazi skladovnega stroja za opis kode programa. */
	public static interface CodeInstr extends Instruction {
	}

	/**
	 * Ukaz skladovnega stroja.
	 */
	public static abstract class INSTR implements Instruction {

		/** Lokacija dela izvorne kode, ki se prevede v ta ukaz. */
		public final Report.Location debugLocation;

		/**
		 * Ustvari nov ukaz skladovnega stroja.
		 * 
		 * @param debugLocation Lokacija dela izvorne kode, ki se prevede v ta ukaz.
		 */
		public INSTR(final Report.Locatable debugLocation) {
			this.debugLocation = debugLocation == null ? null : debugLocation.location();
		}

		@Override
		public Integer size() {
			return 1;
		}

	}

	/**
	 * Oznaka.
	 */
	public static class LABEL extends INSTR implements DataInstr, CodeInstr {

		/** Ime oznake. */
		public final String name;

		/**
		 * Ustvari nov ukaz {@link LABEL}.
		 * 
		 * @param name          Ime oznake.
		 * @param debugLocation Lokacija dela izvorne kode, ki se prevede v ta ukaz.
		 */
		public LABEL(final String name, final Report.Locatable debugLocation) {
			super(debugLocation);
			this.name = name;
		}

		@Override
		public Integer size() {
			return 0;
		}

		@Override
		public String toString() {
			return "LABEL " + name;
		}

	}

	/**
	 * Dodeljevanje prostora v staticnem pomnilniku.
	 */
	public static class SIZE extends INSTR implements DataInstr {

		/** Velikost prostora v pomnilniku. */
		public final Integer size;

		/**
		 * Ustvari nov ukaz {@link SIZE}.
		 * 
		 * @param size          Velikost prostora v pomnilniku.
		 * @param debugLocation Lokacija dela izvorne kode, ki se prevede v ta ukaz.
		 */
		public SIZE(final Integer size, final Report.Locatable debugLocation) {
			super(debugLocation);
			this.size = size;
		}

		@Override
		public Integer size() {
			return size;
		}

		@Override
		public String toString() {
			return "SIZE " + size;
		}

	}

	/**
	 * Konstanta v v staticnem pomnilniku.
	 */
	public static class DATA extends INSTR implements DataInstr {

		/**
		 * Vrednost konstante.
		 */
		public final Integer intc;

		/**
		 * Ustvari nov ukaz {@link SIZE}.
		 * 
		 * @param intc          Vrednost konstante.
		 * @param debugLocation Lokacija dela izvorne kode, ki se prevede v ta ukaz.
		 */
		public DATA(final Integer intc, final Report.Locatable debugLocation) {
			super(debugLocation);
			this.intc = intc;
		}

		@Override
		public Integer size() {
			return 4;
		}

		@Override
		public String toString() {
			return "DATA " + intc;
		}

	}

	/**
	 * Inicializacija spremenljivke.
	 */
	public static class INIT extends INSTR implements CodeInstr {

		/**
		 * Ustvari nok ukaz {@link INIT}.
		 * 
		 * @param debugLocation Lokacija dela izvorne kode, ki se prevede v ta ukaz.
		 */
		public INIT(final Report.Locatable debugLocation) {
			super(debugLocation);
		}

		@Override
		public String toString() {
			return "INIT";
		}

	}

	/**
	 * Prenos vrednosti iz pomnilnika na sklad.
	 */
	public static class LOAD extends INSTR implements CodeInstr {

		/**
		 * Ustvari nok ukaz {@link LOAD}.
		 * 
		 * @param debugLocation Lokacija dela izvorne kode, ki se prevede v ta ukaz.
		 */
		public LOAD(final Report.Locatable debugLocation) {
			super(debugLocation);
		}

		@Override
		public String toString() {
			return "LOAD";
		}

	}

	/**
	 * Prenos vrednosti s sklada v pomnilnik.
	 */
	public static class SAVE extends INSTR implements CodeInstr {

		/**
		 * Ustvari nok ukaz {@link SAVE}.
		 * 
		 * @param debugLocation Lokacija dela izvorne kode, ki se prevede v ta ukaz.
		 */
		public SAVE(final Report.Locatable debugLocation) {
			super(debugLocation);
		}

		@Override
		public String toString() {
			return "SAVE";
		}

	}

	/**
	 * Spreminjanje lokacija vrha sklada.
	 */
	public static class POPN extends INSTR implements CodeInstr {

		/**
		 * Ustvari nok ukaz {@link POPN}.
		 * 
		 * @param debugLocation Lokacija dela izvorne kode, ki se prevede v ta ukaz.
		 */
		public POPN(final Report.Locatable debugLocation) {
			super(debugLocation);
		}

		@Override
		public String toString() {
			return "POPN";
		}

	}

	/**
	 * Prenos konstante na sklad.
	 */
	public static class PUSH extends INSTR implements CodeInstr {

		/** Konstanta. */
		public final Integer intc;

		/**
		 * Ustvari nok ukaz {@link PUSH}.
		 * 
		 * @param intc          Konstanta.
		 * @param debugLocation Lokacija dela izvorne kode, ki se prevede v ta ukaz.
		 */
		public PUSH(final Integer intc, final Report.Locatable debugLocation) {
			super(debugLocation);
			this.intc = intc;
		}

		@Override
		public Integer size() {
			return super.size() + 4;
		}

		@Override
		public String toString() {
			return "PUSH " + intc;
		}

	}

	/**
	 * Prenos imena oznake na sklad.
	 */
	public static class NAME extends INSTR implements CodeInstr {

		/** Ime oznake. */
		public final String name;

		/**
		 * Ustvari nok ukaz {@link NAME}.
		 * 
		 * @param name          Ime oznake.
		 * @param debugLocation Lokacija dela izvorne kode, ki se prevede v ta ukaz.
		 */
		public NAME(final String name, final Report.Locatable debugLocation) {
			super(debugLocation);
			this.name = name;
		}

		@Override
		public Integer size() {
			return super.size() + 4;
		}

		@Override
		public String toString() {
			return "NAME " + name;
		}

	}

	/**
	 * Prenos vrednosti registra na sklad.
	 */
	public static class REGN extends INSTR implements CodeInstr {

		/** Imena registrov. */
		public enum Reg {
			/** Programski stevec. */
			PC,
			/** Klicni kazalec. */
			FP,
			/** Skladovni zapis. */
			SP,
		}

		/** Ime registra. */
		public final Reg regn;

		/**
		 * Ustvari nok ukaz {@link REGN}.
		 * 
		 * @param regn          Ime registra.
		 * @param debugLocation Lokacija dela izvorne kode, ki se prevede v ta ukaz.
		 */
		public REGN(Reg regn, final Report.Locatable debugLocation) {
			super(debugLocation);
			this.regn = regn;
		}

		@Override
		public String toString() {
			return "REGN." + regn;
		}

	}

	/**
	 * Izvedba racunske operacije.
	 */
	public static class OPER extends INSTR implements CodeInstr {

		/** Vrste racunskih operacij. */
		public enum Oper {
			/** Negacija. */
			NOT,
			/** Sprememba predznaka. */
			NEG,
			/** Disjunkcija. */
			OR,
			/** Konjunkcija. */
			AND,
			/** Enakonst. */
			EQU,
			/** Neenakost. */
			NEQ,
			/** Vecji kot. */
			GTH,
			/** Manjsi kot. */
			LTH,
			/** Vecji ali enak. */
			GEQ,
			/** Manjsi ali enak. */
			LEQ,
			/** Sestevanje. */
			ADD,
			/** Odstevanje. */
			SUB,
			/** Mnozenje. */
			MUL,
			/** Deljenje. */
			DIV,
			/** Modulo. */
			MOD,
		}

		/** Racunska operacija. */
		public final Oper oper;

		/**
		 * Ustvari nok ukaz {@link OPER}.
		 * 
		 * @param oper          Racunska operacija.
		 * @param debugLocation Lokacija dela izvorne kode, ki se prevede v ta ukaz.
		 */
		public OPER(final Oper oper, final Report.Locatable debugLocation) {
			super(debugLocation);
			this.oper = oper;
		}

		@Override
		public String toString() {
			return "OPER." + oper;
		}

	}

	/**
	 * Brezpogojni skok.
	 */
	public static class UJMP extends INSTR implements CodeInstr {

		/**
		 * Ustvari nok ukaz {@link UJMP}.
		 * 
		 * @param debugLocation Lokacija dela izvorne kode, ki se prevede v ta ukaz.
		 */
		public UJMP(final Report.Locatable debugLocation) {
			super(debugLocation);
		}

		@Override
		public String toString() {
			return "UJMP";
		}

	}

	/**
	 * Pogojni skok.
	 */
	public static class CJMP extends INSTR implements CodeInstr {

		/**
		 * Ustvari nok ukaz {@link CJMP}.
		 * 
		 * @param debugLocation Lokacija dela izvorne kode, ki se prevede v ta ukaz.
		 */
		public CJMP(final Report.Locatable debugLocation) {
			super(debugLocation);
		}

		@Override
		public String toString() {
			return "CJMP";
		}

	}

	/**
	 * Klic podprograma.
	 */
	public static class CALL extends INSTR implements CodeInstr {

		/** Klicni zapis klicanega podprograma. */
		public final Mem.Frame debugFrame;

		/**
		 * Ustvari nok ukaz {@link CALL}.
		 * 
		 * @param debugFrame    Klicni zapis klicanega podprograma.
		 * @param debugLocation Lokacija dela izvorne kode, ki se prevede v ta ukaz.
		 */
		public CALL(final Mem.Frame debugFrame, final Report.Locatable debugLocation) {
			super(debugLocation);
			this.debugFrame = debugFrame;
		}

		@Override
		public String toString() {
			return "CALL";
		}

	}

	/**
	 * Vrnitev iz podprograma.
	 */
	public static class RETN extends INSTR implements CodeInstr {

		/** Klicni zapis vracajocega se podprograma. */
		public final Mem.Frame debugFrame;

		/**
		 * Ustvari nok ukaz {@link RETN}.
		 * 
		 * @param debugFrame    Klicni zapis vracajocega se programa.
		 * @param debugLocation Lokacija dela izvorne kode, ki se prevede v ta ukaz.
		 */
		public RETN(final Mem.Frame debugFrame, final Report.Locatable debugLocation) {
			super(debugLocation);
			this.debugFrame = debugFrame;
		}

		@Override
		public String toString() {
			return "RETN";
		}

	}

}