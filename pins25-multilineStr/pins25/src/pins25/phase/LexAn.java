package pins25.phase;

import java.io.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import pins25.common.*;
import pins25.common.Report.Location;
import pins25.common.Token.Symbol;

/**
 * Leksikalni analizator.
 */
public class LexAn implements AutoCloseable {

	/** Izvorna datoteka. */
	private final Reader srcFile;

	/**
	 * Ustvari nov leksikalni analizator.
	 * 
	 * @param srcFileName Ime izvorne datoteke.
	 */
	public LexAn(final String srcFileName) {
		try {
			srcFile = new BufferedReader(new InputStreamReader(new FileInputStream(new File(srcFileName))));
			nextChar(); // Pripravi prvi znak izvorne datoteke (glej {@link nextChar}).
		} catch (FileNotFoundException __) {
			throw new Report.Error("Source file '" + srcFileName + "' not found.");
		}
	}

	@Override
	public void close() {
		try {
			srcFile.close();
		} catch (IOException __) {
			throw new Report.Error("Cannot close source file.");
		}
	}

	/** Trenutni znak izvorne datoteke (glej {@link nextChar}). */
	private int buffChar = -2;

	/** Vrstica trenutnega znaka izvorne datoteke (glej {@link nextChar}). */
	private int buffCharLine = 0;

	/** Stolpec trenutnega znaka izvorne datoteke (glej {@link nextChar}). */
	private int buffCharColumn = 0;

	/**
	 * Prebere naslednji znak izvorne datoteke.
	 * 
	 * Izvorno datoteko beremo znak po znak. Trenutni znak izvorne datoteke je
	 * shranjen v spremenljivki {@link buffChar}, vrstica in stolpec trenutnega
	 * znaka izvorne datoteke sta shranjena v spremenljivkah {@link buffCharLine} in
	 * {@link buffCharColumn}.
	 * 
	 * Zacetne vrednosti {@link buffChar}, {@link buffCharLine} in
	 * {@link buffCharColumn} so {@code '\n'}, {@code 0} in {@code 0}: branje prvega
	 * znaka izvorne datoteke bo na osnovi vrednosti {@code '\n'} spremenljivke
	 * {@link buffChar} prvemu znaku izvorne datoteke priredilo vrstico 1 in stolpec
	 * 1.
	 * 
	 * Pri branju izvorne datoteke se predpostavlja, da je v spremenljivki
	 * {@link buffChar} ves "cas veljaven znak. Zunaj metode {@link nextChar} so vse
	 * spremenljivke {@link buffChar}, {@link buffCharLine} in
	 * {@link buffCharColumn} namenjene le branju.
	 * 
	 * Vrednost {@code -1} v spremenljivki {@link buffChar} pomeni konec datoteke
	 * (vrednosti spremenljivk {@link buffCharLine} in {@link buffCharColumn} pa
	 * nista ve"c veljavni).
	 */
	private void nextChar() {
		try {
			switch (buffChar) {
				case -2: // Noben znak "se ni bil prebran.
					buffChar = srcFile.read();
					buffCharLine = buffChar == -1 ? 0 : 1;
					buffCharColumn = buffChar == -1 ? 0 : 1;
					return;
				case -1: // Konec datoteke je bil "ze viden.
					return;
				case '\n': // Prejsnji znak je koncal vrstico, zacne se nova vrstica.
					buffChar = srcFile.read();
					buffCharLine = buffChar == -1 ? buffCharLine : buffCharLine + 1;
					buffCharColumn = buffChar == -1 ? buffCharColumn : 1;
					return;
				case '\t': // Prejsnji znak je tabulator, ta znak je morda potisnjen v desno.
					buffChar = srcFile.read();
					while (buffCharColumn % 4 != 0)
						buffCharColumn += 1;
					buffCharColumn += 1;
					return;
				default: // Prejsnji znak je brez posebnosti.
					buffChar = srcFile.read();
					buffCharColumn += 1;
					return;
			}
		} catch (IOException __) {
			throw new Report.Error("Cannot read source file.");
		}
	}

	/**
	 * Trenutni leksikalni simbol.
	 * 
	 * "Ce vrednost spremenljivke {@code buffToken} ni {@code null}, je simbol "ze
	 * prebran iz vhodne datoteke, ni pa "se predan naprej sintaksnemu analizatorju.
	 * Ta simbol je dostopen z metodama {@link peekToken} in {@link takeToken}.
	 */
	private Token buffToken = null;

	/**
	 * Prebere naslednji leksikalni simbol, ki je nato dostopen preko metod
	 * {@link peekToken} in {@link takeToken}.
	 */
	private void nextToken() {

		if (Character.isWhitespace(buffChar))
			skipWhitspace();

		if (buffChar == -1)
			buffToken = new Token(new Location(0, 0), Symbol.EOF, "");
		else if ("=,&|!><+-*/%^()".indexOf((char) buffChar) != -1)
			isOperator();
		else if (Character.isLetter(buffChar) || (char) buffChar == '_')
			isKeywordOrIdentifier();
		else if (Character.isDigit(buffChar))
			isDigitConst(new StringBuilder(Character.toString(buffChar)), buffCharLine, buffCharColumn);
		else if ('"' == (char) buffChar)
			isStringConst();
		else if ('\'' == (char) buffChar)
			isCharConst();
		else
			throw new Report.Error(errorMsg("Invalid character"));

		/*** TODO ***/
	}

	private void isOperator() {

		int begLine = buffCharLine;
		int begCol = buffCharColumn;

		StringBuilder sb = new StringBuilder(Character.toString(buffChar));

		nextChar();
		if (!Character.isWhitespace(buffChar) && buffChar != -1) {
			sb.append(Character.toString(buffChar));
		}

		Matcher m = OPE_P.matcher(sb.toString());
		if (m.find()) {
			String tmp = m.group();
			if (tmp.equals("//")) {
				try {
					skipComment();
				} catch (Exception e) {
					throw new Report.Error(new Location(buffCharLine, buffCharColumn),
							"SyntaxError: Invalid or unexpected token");
				}
				return;
			} else {
				buffToken = new Token(
						tmp.length() > 1 ? new Location(begLine, begCol, buffCharLine, buffCharColumn)
								: new Location(begLine, begCol),
						getSymbol(tmp),
						tmp);
			}
			if (tmp.length() > 1)
				nextChar();
		}
	}

	private Symbol getSymbol(String s) {
		switch (s) {
			case "=":
				return Symbol.ASSIGN;
			case ",":
				return Symbol.COMMA;
			case "&&":
				return Symbol.AND;
			case "||":
				return Symbol.OR;
			case "!":
				return Symbol.NOT;
			case "==":
				return Symbol.EQU;
			case "!=":
				return Symbol.NEQ;
			case ">":
				return Symbol.GTH;
			case "<":
				return Symbol.LTH;
			case ">=":
				return Symbol.GEQ;
			case "<=":
				return Symbol.LEQ;
			case "(":
				return Symbol.LPAREN;
			case ")":
				return Symbol.RPAREN;
			case "+":
				return Symbol.ADD;
			case "-":
				return Symbol.SUB;
			case "*":
				return Symbol.MUL;
			case "/":
				return Symbol.DIV;
			case "%":
				return Symbol.MOD;
			case "^":
				return Symbol.PTR;
			case "fun":
				return Symbol.FUN;
			case "var":
				return Symbol.VAR;
			case "if":
				return Symbol.IF;
			case "then":
				return Symbol.THEN;
			case "else":
				return Symbol.ELSE;
			case "while":
				return Symbol.WHILE;
			case "do":
				return Symbol.DO;
			case "let":
				return Symbol.LET;
			case "in":
				return Symbol.IN;
			case "end":
				return Symbol.END;
			default:
				return Symbol.IDENTIFIER;
		}
	}

	private void skipWhitspace() {
		while (Character.isWhitespace(buffChar)) {
			nextChar();
		}
	}

	private void skipComment() throws Exception {

		nextChar();
		if ((char) buffChar == '{')
			skipMultilineComment();
		else {
			while ((char) buffChar != '\n' && buffChar != -1) {
				nextChar();
			}
		}
		nextChar();
		if (buffChar == -1) {
			buffToken = new Token(new Location(0, 0), Symbol.EOF, "");
		} else
			nextToken();

	}

	private void skipMultilineComment() throws Exception {
		StringBuilder buf = new StringBuilder();
		while (buffChar != -1) {
			if ((char) buffChar == '#' || (char) buffChar == '/') {
				buf.append((char) buffChar);
				if (buf.toString().equals("//#")) {
					skipMultilineComment();
				} else if (buf.toString().equals("#//")) {
					break;
				} else if (buf.length() == 3)
					buf = new StringBuilder();
			} else
				buf = new StringBuilder();
		}
		if (buffChar == -1)
			throw new Exception("Placeholder");
	}

	private void isKeywordOrIdentifier() {

		int begLine = buffCharLine;
		int begCol = buffCharColumn;

		int endLine = 0;
		int endCol = 0;
		StringBuilder sb = new StringBuilder();

		while (Character.isLetterOrDigit(buffChar) || (char) buffChar == '_') {
			endLine = buffCharLine;
			endCol = buffCharColumn;
			sb.append((char) buffChar);
			nextChar();
		}
		if (buffChar != -1 && "=,!><|&+-*/%^()".indexOf((char) buffChar) == -1 && !Character.isWhitespace(buffChar)) {
			throw new Report.Error(errorMsg("Invalid character"));
		}
		buffToken = new Token(new Location(begLine, begCol, endLine, endCol), getSymbol(sb.toString()), sb.toString());
	}

	private void isDigitConst(StringBuilder sb, int begLine, int begCol) {

		int endLine = buffCharLine;
		int endCol = buffCharColumn;

		nextChar();
		while (Character.isDigit(buffChar)) {
			endLine = buffCharLine;
			endCol = buffCharColumn;
			sb.append(Character.toString(buffChar));
			nextChar();
		}
		String tmp = "+-".indexOf(sb.charAt(0)) == -1 ? sb.toString() : sb.toString().substring(1);
		if (tmp.length() > 1 && tmp.charAt(0) == '0') {
			throw new Report.Error(errorMsg("Number can't start with 0 while being longer than 1 digit"));
		}
		buffToken = new Token(new Location(begLine, begCol, endLine, endCol), Symbol.INTCONST,
				sb.toString());

	}

	private void isStringConst() {

		int begLine = buffCharLine;
		int begCol = buffCharColumn;

		StringBuilder sb = new StringBuilder();
		sb.append((char) buffChar);

		nextChar();
		while ('"' != ((char) buffChar) && (char) buffChar != -1) {
			if (Character.toString(buffChar) == "\\") {
				nextChar();
				if ((char) buffChar == 'n')
					sb.append("\n");
				else if ((char) buffChar == '"')
					sb.append("\"");
				else if ((char) buffChar == '\\')
					sb.append("\\");
				else if (isHex(Character.toString(buffChar))) {
					sb.append('\\' + Character.toString(buffChar));
					nextChar();
					if (isHex(Character.toString(buffChar)))
						sb.append(Character.toString(buffChar));
					else
						throw new Report.Error(errorMsg("Expected hex got"));
				} else
					throw new Report.Error(errorMsg("Invalid escaped character"));
			} else if (buffChar >= 32 && buffChar <= 126) {
				sb.append((char) buffChar);
			} else {
				throw new Report.Error(errorMsg("Invalid character"));
			}
			nextChar();
		}

		if (buffChar == -1) {
			throw new Report.Error(errorMsg("Unfinished string literal"));
		}
		sb.append((char) buffChar);

		buffToken = new Token(new Location(begLine, begCol, buffCharLine, buffCharColumn), Symbol.STRINGCONST,
				sb.toString());
		// check for multiline string
		if (sb.toString().equals("\"\"")) {
			MYB_multilineString(sb, begLine, begCol);
		} else {
			nextChar();
		}

	}

	private void MYB_multilineString(StringBuilder sb, int begLine, int begCol) {
		nextChar();

		if ((char) buffChar == '\"') {
			sb.append((char) buffChar);
			var sbEnd = new StringBuilder();
			while (!sbEnd.toString().equals("\"\"\"") && buffChar != -1) {
				nextChar();
				if ((char) buffChar == '\"') {
					sbEnd.append((char) buffChar);
				} else {
					sbEnd = new StringBuilder();
				}
				sb.append((char) buffChar);
			}
			if (buffChar == -1) {
				errorMsg("Unexpected end");
			}
			buffToken = new Token(new Location(begLine, begCol, buffCharLine, buffCharColumn), Symbol.STRINGCONST,
					sb.toString());
			nextChar();
		}
	}

	private void isCharConst() {
		StringBuilder sb = new StringBuilder();

		int begLine = buffCharLine;
		int begCol = buffCharColumn;

		sb.append((char) buffChar);

		nextChar();
		if ((char) buffChar == '\\') { // is escapable
			nextChar();
			sb.append('\\');
			if ((char) buffChar == 'n')
				sb.append('n');
			else if ((char) buffChar == '\'')
				sb.append('\'');
			else if ((char) buffChar == '\\')
				sb.append('\\');
			else if (isHex(Character.toString(buffChar))) {
				sb.append((char) buffChar);
				nextChar();
				if (isHex(Character.toString(buffChar)))
					sb.append((char) buffChar);
				else
					throw new Report.Error(errorMsg("Expected hex got"));
			} else
				throw new Report.Error(errorMsg("Invalid escaped character"));
		} else if (buffChar >= 32 && buffChar <= 126) { // is normal ASCII
			sb.append((char) buffChar);
		} else {
			throw new Report.Error(errorMsg("Invalid character"));
		}
		nextChar();
		if ((char) buffChar != '\'') {
			throw new Report.Error(errorMsg("Expected \' got "));
		}
		sb.append((char) buffChar);
		buffToken = new Token(new Location(begLine, begCol, buffCharLine, buffCharColumn), Symbol.CHARCONST,
				sb.toString());
		nextChar();
	}

	private boolean isHex(String c) { // Mal shabby hex matcher
		return HEX_P.matcher(c).find();
	}

	/**
	 * Vrne trenutni leksikalni simbol, ki ostane v lastnistvu leksikalnega
	 * analizatorja.
	 * 
	 * @return Leksikalni simbol.
	 */
	public Token peekToken() {
		if (buffToken == null)
			nextToken();
		return buffToken;
	}

	/**
	 * Vrne trenutni leksikalni simbol, ki preide v lastnistvo klicoce kode.
	 * 
	 * @return Leksikalni simbol.
	 */
	public Token takeToken() {
		if (buffToken == null)
			nextToken();
		final Token thisToken = buffToken;
		buffToken = null;
		return thisToken;
	}

	// --- ZAGON ---

	/**
	 * Zagon leksikalnega analizatorja kot samostojnega programa.
	 * 
	 * @param cmdLineArgs Argumenti v ukazni vrstici.
	 */
	public static void main(final String[] cmdLineArgs) {
		System.out.println("This is PINS'25 compiler (lexical analysis):");

		try {
			if (cmdLineArgs.length == 0)
				throw new Report.Error("No source file specified in the command line.");
			if (cmdLineArgs.length > 1)
				Report.warning("Unused arguments in the command line.");

			try (LexAn lexAn = new LexAn(cmdLineArgs[0])) {
				while (lexAn.peekToken().symbol() != Token.Symbol.EOF)
					System.out.println(lexAn.takeToken());
				System.out.println(lexAn.takeToken());
			}

			// Upajmo, da kdaj pridemo to te tocke.
			// A zavedajmo se sledecega:
			// 1. Prevod je zaradi napak v programu lahko napacen :-o
			// 2. Izvorni program se zdalec ni tisto, kar je programer hotel, da bi bil ;-)
			Report.info("Done.");
		} catch (Report.Error error) {
			// Izpis opisa napake.
			System.err.println(error.getMessage());
			System.exit(1);
		}
	}

	private String errorMsg(String c) {
		return String.format("[%d, %d] %s %c", buffCharLine, buffCharColumn, c, (char) buffChar);
	}

	private final Pattern OPE_P = Pattern.compile(
			"^(==|!=|>=|<=|&&|\\|\\||//|[=,!><+\\-*/%^()])");

	private final Pattern HEX_P = Pattern.compile(
			"[a-f0-9]");

}