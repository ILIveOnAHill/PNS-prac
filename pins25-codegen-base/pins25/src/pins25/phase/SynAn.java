package pins25.phase;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import pins25.common.*;
import pins25.common.Token.Symbol;

/**
 * Sintaksni analizator.
 */
public class SynAn implements AutoCloseable {

	/** Leksikalni analizator. */
	private final LexAn lexAn;

	/**
	 * Ustvari nov sintaksni analizator.
	 * 
	 * @param srcFileName Ime izvorne datoteke.
	 */
	public SynAn(final String srcFileName) {
		this.lexAn = new LexAn(srcFileName);
	}

	@Override
	public void close() {
		lexAn.close();
	}

	/**
	 * Prevzame leksikalni analizator od leksikalnega analizatorja in preveri, ali
	 * je prave vrste.
	 * 
	 * @param symbol Pricakovana vrsta leksikalnega simbola.
	 * @return Prevzeti leksikalni simbol.
	 */
	private Token check(Token.Symbol symbol) {
		final Token token = lexAn.takeToken();
		if (token.symbol() != symbol)
			throw new Report.Error(token, "Unexpected symbol '" + token.lexeme() + "'.");
		return token;
	}

	/**
	 * Opravi sintaksno analizo.
	 */
	private HashMap<AST.Node, Report.Locatable> attrLoc;

	public AST.Node parse(HashMap<AST.Node, Report.Locatable> attrLoc) {
		this.attrLoc = attrLoc;
		final AST.Nodes<AST.MainDef> defs = parseProg();
		if (lexAn.peekToken().symbol() != Token.Symbol.EOF)
			Report.warning(lexAn.peekToken(),
					"Unexpected text '" + lexAn.peekToken().lexeme() + "...' at the end of the program.");
		return defs;
	}

	private Symbol next() {
		return lexAn.peekToken().symbol();
	}

	private AST.Nodes<AST.MainDef> parseProg() {

		var defs = new ArrayList<AST.MainDef>();

		tmp: while (true) {
			defs.add(parseDef());
			switch (next()) {
				case FUN:
					break;
				case VAR:
					break;
				default:
					break tmp;
			}
		}

		return new AST.Nodes<AST.MainDef>(defs);
	}

	private AST.Nodes<AST.MainDef> MYB_Def() {

		var defs = new ArrayList<AST.MainDef>();

		tmp: while (true) {
			defs.add(parseDef());
			switch (next()) {
				case FUN:
					break;
				case VAR:
					break;
				default:
					break tmp;
			}
		}
		return new AST.Nodes<AST.MainDef>(defs);
	}

	private AST.MainDef parseDef() {

		AST.MainDef def = null;

		Token t = lexAn.peekToken();
		switch (next()) {
			case FUN:
				check(Symbol.FUN);
				String n = check(Symbol.IDENTIFIER).lexeme();
				check(Symbol.LPAREN);
				var params = parseParams();
				Token rp = check(Symbol.RPAREN);
				var stmts = def2();
				def = new AST.FunDef(n, params.getAll(), stmts.getAll());
				attrLoc.put(def, new Report.Location(
						t.location().begLine(), t.location().begColumn(),
						stmts.getAll().isEmpty() ? rp.location().endLine() : getEndLine(stmts.getAll()),
						stmts.getAll().isEmpty() ? rp.location().endColumn() : getEndColumn(stmts.getAll())));
				break;
			case VAR:
				check(Symbol.VAR);
				n = check(Symbol.IDENTIFIER).lexeme();
				Token a = check(Symbol.ASSIGN);
				AST.Nodes<AST.Init> intis = parseInits();
				def = new AST.VarDef(n, intis.getAll());
				attrLoc.put(def, new Report.Location(
						t.location().begLine(), t.location().begColumn(),
						intis.getAll().isEmpty() ? a.location().endLine() : getEndLine(intis.getAll()),
						intis.getAll().isEmpty() ? a.location().endColumn() : getEndColumn(intis.getAll())));
				break;
			default:
				error();
				break;
		}

		return def;
	}

	private AST.Nodes<AST.Stmt> def2() {

		switch (next()) {
			case ASSIGN:
				check(Symbol.ASSIGN);
				return parseStmts();
			default:
				return new AST.Nodes<AST.Stmt>();
		}
	}

	private AST.Nodes<AST.ParDef> parseParams() {

		var params = new ArrayList<AST.ParDef>();

		if (next() != Symbol.RPAREN) {
			bp: while (true) {
				Token id = check(Symbol.IDENTIFIER);
				var param = new AST.ParDef(id.lexeme());
				params.add(param);
				attrLoc.put(param, id.location());
				switch (next()) {
					case COMMA:
						check(Symbol.COMMA);
						break;
					case RPAREN:
						break bp;
					default:
						error();
						break;
				}
			}
		}
		return new AST.Nodes<AST.ParDef>(params);
	}

	private AST.Nodes<AST.Stmt> parseStmts() {

		var stmts = new ArrayList<AST.Stmt>();
		tmp: while (true) {
			stmts.add(parseStmt());
			switch (next()) {
				case COMMA:
					check(Symbol.COMMA);
					break;
				case RPAREN:
					break tmp;
				case END:
					break tmp;
				case ELSE:
					break tmp;
				case EOF:
					break tmp;
				case FUN:
					break tmp;
				case VAR:
					break tmp;
				case IN:
					break tmp;
				default:
					error();
					break;
			}
		}
		return new AST.Nodes<AST.Stmt>(stmts);
	}

	private AST.Stmt parseStmt() {
		Token t = lexAn.peekToken();
		switch (next()) {
			case IF:
				check(Symbol.IF);
				var exp = parseExp();
				check(Symbol.THEN);
				var stmts = parseStmts();
				var elseStmt = parseElse();
				Token e = check(Symbol.END);
				var node = new AST.IfStmt(exp, stmts.getAll(), elseStmt.getAll());
				attrLoc.put(node, new Report.Location(
						t.location().begLine(), t.location().begColumn(),
						e.location().endLine(), e.location().endColumn()));
				return node;
			case WHILE:
				t = check(Symbol.WHILE);
				exp = parseExp();
				check(Symbol.DO);
				stmts = parseStmts();
				e = check(Symbol.END);
				var wNode = new AST.WhileStmt(exp, stmts.getAll());
				attrLoc.put(wNode, new Report.Location(
						t.location().begLine(), t.location().begColumn(),
						e.location().endLine(), e.location().endColumn()));
				return wNode;
			case LET:
				t = check(Symbol.LET);
				var defs = MYB_Def();
				check(Symbol.IN);
				stmts = parseStmts();
				e = check(Symbol.END);
				var lNode = new AST.LetStmt(defs.getAll(), stmts.getAll());
				attrLoc.put(lNode, new Report.Location(
						t.location().begLine(), t.location().begColumn(),
						e.location().endLine(), e.location().endColumn()));
				return lNode;
			default:
				exp = parseExp();
				var eNode = parseStmt2(exp);
				return eNode;
		}
	}

	private AST.Nodes<AST.Stmt> parseElse() {
		switch (next()) {
			case ELSE:
				check(Symbol.ELSE);
				return parseStmts();
			default:
				return new AST.Nodes<AST.Stmt>();
		}
	}

	private AST.Stmt parseStmt2(AST.Expr exp) {
		switch (next()) {
			case ASSIGN:
				check(Symbol.ASSIGN);
				var exp2 = parseExp();
				var aStmt = new AST.AssignStmt(exp, exp2);
				attrLoc.put(aStmt, new Report.Location(
						attrLoc.get(exp), attrLoc.get(exp2)));
				return aStmt;
			default:
				var expS = new AST.ExprStmt(exp);
				attrLoc.put(expS, attrLoc.get(exp));
				return expS;
		}
	}

	private AST.Expr parseExp() {
		var firstExp = parseExp2();
		return MYB_OR(firstExp);
	}

	private AST.Expr MYB_OR(AST.Expr firstExp) {
		switch (next()) {
			case OR:
				check(Symbol.OR);
				var secondExp = parseExp2();
				var binExp = new AST.BinExpr(AST.BinExpr.Oper.OR, firstExp, secondExp);
				attrLoc.put(binExp, new Report.Location(attrLoc.get(firstExp), attrLoc.get(secondExp)));
				return MYB_OR(binExp);
			default:
				return firstExp;
		}
	}

	private AST.Expr parseExp2() {
		var firstExp = parseExp3();
		return MYB_AND(firstExp);
	}

	private AST.Expr MYB_AND(AST.Expr firstExp) {
		switch (next()) {
			case AND:
				check(Symbol.AND);
				var secondExp = parseExp3();
				var binExp = new AST.BinExpr(AST.BinExpr.Oper.AND, firstExp, secondExp);
				attrLoc.put(binExp, new Report.Location(attrLoc.get(firstExp), attrLoc.get(secondExp)));
				return MYB_AND(binExp);
			default:
				return firstExp;
		}
	}

	private AST.Expr parseExp3() {
		var firstExp = parseExp4();
		return checkForCompareOP(firstExp);
	}

	private AST.Expr checkForCompareOP(AST.Expr firstExp) {
		switch (next()) {
			case EQU:
				check(Symbol.EQU);
				var secondExp = parseExp4();
				var binExp = new AST.BinExpr(AST.BinExpr.Oper.EQU, firstExp, secondExp);
				attrLoc.put(binExp, new Report.Location(attrLoc.get(firstExp), attrLoc.get(secondExp)));
				return binExp;
			case NEQ:
				check(Symbol.NEQ);
				secondExp = parseExp4();
				binExp = new AST.BinExpr(AST.BinExpr.Oper.NEQ, firstExp, secondExp);
				attrLoc.put(binExp, new Report.Location(attrLoc.get(firstExp), attrLoc.get(secondExp)));
				return binExp;
			case LTH:
				check(Symbol.LTH);
				secondExp = parseExp4();
				binExp = new AST.BinExpr(AST.BinExpr.Oper.LTH, firstExp, secondExp);
				attrLoc.put(binExp, new Report.Location(attrLoc.get(firstExp), attrLoc.get(secondExp)));
				return binExp;
			case GTH:
				check(Symbol.GTH);
				secondExp = parseExp4();
				binExp = new AST.BinExpr(AST.BinExpr.Oper.GTH, firstExp, secondExp);
				attrLoc.put(binExp, new Report.Location(attrLoc.get(firstExp), attrLoc.get(secondExp)));
				return binExp;
			case LEQ:
				check(Symbol.LEQ);
				secondExp = parseExp4();
				binExp = new AST.BinExpr(AST.BinExpr.Oper.LEQ, firstExp, secondExp);
				attrLoc.put(binExp, new Report.Location(attrLoc.get(firstExp), attrLoc.get(secondExp)));
				return binExp;
			case GEQ:
				check(Symbol.GEQ);
				secondExp = parseExp4();
				binExp = new AST.BinExpr(AST.BinExpr.Oper.GEQ, firstExp, secondExp);
				attrLoc.put(binExp, new Report.Location(attrLoc.get(firstExp), attrLoc.get(secondExp)));
				return binExp;
			default:
				return firstExp;
		}
	}

	private AST.Expr parseExp4() {
		var firstExp = parseExp5();
		return checkForAdditiveOP(firstExp);
	}

	private AST.Expr checkForAdditiveOP(AST.Expr firstExp) { // mores razdelit vse za operatorje.
		switch (next()) {
			case ADD:
				check(Symbol.ADD);
				var secondExp = parseExp5();
				var binExp = new AST.BinExpr(AST.BinExpr.Oper.ADD, firstExp, secondExp);
				attrLoc.put(binExp, new Report.Location(attrLoc.get(firstExp), attrLoc.get(secondExp)));
				return checkForAdditiveOP(binExp);
			case SUB:
				check(Symbol.SUB);
				secondExp = parseExp5();
				binExp = new AST.BinExpr(AST.BinExpr.Oper.SUB, firstExp, secondExp);
				attrLoc.put(binExp, new Report.Location(attrLoc.get(firstExp), attrLoc.get(secondExp)));
				return checkForAdditiveOP(binExp);
			default:
				return firstExp;
		}
	}

	private AST.Expr parseExp5() {
		var firstExp = parseExp6();
		return checkForMultiplicativeOP(firstExp);

	}

	private AST.Expr checkForMultiplicativeOP(AST.Expr firstExp) {
		switch (next()) {
			case MUL:
				check(Symbol.MUL);
				var secondExp = parseExp6();
				var binExp = new AST.BinExpr(AST.BinExpr.Oper.MUL, firstExp, secondExp);
				attrLoc.put(binExp, new Report.Location(attrLoc.get(firstExp), attrLoc.get(secondExp)));
				return checkForMultiplicativeOP(binExp);
			case DIV:
				check(Symbol.DIV);
				secondExp = parseExp6();
				binExp = new AST.BinExpr(AST.BinExpr.Oper.DIV, firstExp, secondExp);
				attrLoc.put(binExp, new Report.Location(attrLoc.get(firstExp), attrLoc.get(secondExp)));
				return checkForMultiplicativeOP(binExp);
			case MOD:
				check(Symbol.MOD);
				secondExp = parseExp6();
				binExp = new AST.BinExpr(AST.BinExpr.Oper.MOD, firstExp, secondExp);
				attrLoc.put(binExp, new Report.Location(attrLoc.get(firstExp), attrLoc.get(secondExp)));
				return checkForMultiplicativeOP(binExp);
			default:
				return firstExp;
		}
	}

	private AST.Expr parseExp6() {
		return checkForPrefixOP();
	}

	private AST.Expr checkForPrefixOP() {
		Token t = lexAn.peekToken();
		switch (next()) {
			case NOT:
				check(Symbol.NOT);
				var exp = checkForPrefixOP();
				var unExp = new AST.UnExpr(AST.UnExpr.Oper.NOT, exp);
				attrLoc.put(unExp, new Report.Location(t.location(), attrLoc.get(exp)));
				return unExp;
			case PTR:
				check(Symbol.PTR);
				exp = checkForPrefixOP();
				unExp = new AST.UnExpr(AST.UnExpr.Oper.MEMADDR, exp);
				attrLoc.put(unExp, new Report.Location(t.location(), attrLoc.get(exp)));
				return unExp;
			case ADD:
				check(Symbol.ADD);
				exp = checkForPrefixOP();
				unExp = new AST.UnExpr(AST.UnExpr.Oper.ADD, exp);
				attrLoc.put(unExp, new Report.Location(t.location(), attrLoc.get(exp)));
				return unExp;
			case SUB:
				check(Symbol.SUB);
				exp = checkForPrefixOP();
				unExp = new AST.UnExpr(AST.UnExpr.Oper.SUB, exp);
				attrLoc.put(unExp, new Report.Location(t.location(), attrLoc.get(exp)));
				return unExp;
			default:
				return parseExp7();
		}
	}

	private AST.Expr checkForPostfix(AST.Expr atomExpr) {

		tmp: while (true) {
			switch (next()) {
				case PTR:
					Token t = check(Symbol.PTR);
					var unExpr = new AST.UnExpr(AST.UnExpr.Oper.VALUEAT, atomExpr);
					attrLoc.put(unExpr, new Report.Location(attrLoc.get(atomExpr), t));
					atomExpr = unExpr;
					break;
				default:
					break tmp;
			}
		}

		return atomExpr;
	}

	private AST.Expr parseExp7() {
		var exp = parseAtom();
		return checkForPostfix(exp);
	}

	private AST.Expr parseAtom() {

		AST.Expr atomExp = null;

		Token t = lexAn.peekToken();

		switch (next()) {
			case INTCONST:
				check(Symbol.INTCONST);
				atomExp = new AST.AtomExpr(AST.AtomExpr.Type.INTCONST, t.lexeme());
				attrLoc.put(atomExp, t.location());
				break;
			case STRINGCONST:
				check(Symbol.STRINGCONST);
				atomExp = new AST.AtomExpr(AST.AtomExpr.Type.STRCONST, t.lexeme());
				attrLoc.put(atomExp, t.location());
				break;
			case CHARCONST:
				check(Symbol.CHARCONST);
				atomExp = new AST.AtomExpr(AST.AtomExpr.Type.CHRCONST, t.lexeme());
				attrLoc.put(atomExp, t.location());
				break;
			case IDENTIFIER:
				check(Symbol.IDENTIFIER);
				atomExp = MYB_Args(t);
				break;
			case LPAREN:
				check(Symbol.LPAREN);
				atomExp = parseExp();
				Token rp = check(Symbol.RPAREN);
				attrLoc.put(atomExp, new Report.Location(t, rp));
				break;
			default:
				error();
		}

		return atomExp;
	}

	private AST.Expr MYB_Args(Token t) {
		switch (next()) {
			case LPAREN:
				check(Symbol.LPAREN);
				var args = parseArgs();
				Token rp = check(Symbol.RPAREN);
				var cExp = new AST.CallExpr(t.lexeme(), args.getAll());
				attrLoc.put(cExp, new Report.Location(t.location(), rp.location()));
				return cExp;
			default:
				var id = new AST.VarExpr(t.lexeme());
				attrLoc.put(id, t);
				return id;
		}
	}

	private AST.Nodes<AST.Expr> parseArgs() {

		var args = new ArrayList<AST.Expr>();

		if (next() != Symbol.RPAREN) {
			tmp: while (true) {
				args.add(parseExp());
				switch (next()) {
					case COMMA:
						check(Symbol.COMMA);
						break;
					case RPAREN:
						break tmp;
					default:
						error();
						break;
				}
			}
		}
		return new AST.Nodes<AST.Expr>(args);
	}

	private AST.Nodes<AST.Init> parseInits() {

		var inits = new ArrayList<AST.Init>();

		tmp: while (true) {
			inits.add(parseInit());
			switch (next()) {
				case COMMA:
					check(Symbol.COMMA);
					break;
				case EOF:
					break tmp;
				case VAR:
					break tmp;
				case FUN:
					break tmp;
				case IN:
					break tmp;
				default:
					error();
			}
		}

		return new AST.Nodes<AST.Init>(inits);
	}

	private AST.Init parseInit() {

		AST.Init init = null;
		switch (next()) {
			case INTCONST:
				Token t = check(Symbol.INTCONST);
				var exp = new AST.AtomExpr(AST.AtomExpr.Type.INTCONST, t.lexeme());
				attrLoc.put(exp, t.location());
				init = MYB_MUL(exp, t);
				break;
			case STRINGCONST:
				t = check(Symbol.STRINGCONST);
				exp = new AST.AtomExpr(AST.AtomExpr.Type.STRCONST, t.lexeme());
				attrLoc.put(exp, t.location());
				init = new AST.Init(genOneRepAtomExpr(t), exp);
				attrLoc.put(init, t.location());
				break;
			case CHARCONST:
				t = check(Symbol.CHARCONST);
				exp = new AST.AtomExpr(AST.AtomExpr.Type.CHRCONST, t.lexeme());
				attrLoc.put(exp, t.location());
				init = new AST.Init(genOneRepAtomExpr(t), exp);
				attrLoc.put(init, t.location());
				break;
			default:
				error();
				break;
		}
		return init;
	}

	private AST.AtomExpr genOneRepAtomExpr(Token t) {
		var num = new AST.AtomExpr(AST.AtomExpr.Type.INTCONST, "1");
		attrLoc.put(num, t.location());
		return num;
	}

	private AST.Init MYB_MUL(AST.AtomExpr exp, Token t) {
		switch (next()) {
			case MUL:
				check(Symbol.MUL);
				var init = new AST.Init(exp, checkConst());
				Token endT = check(next());
				attrLoc.put(init, new Report.Location(
						t.location(), endT.location()));
				return init;
			default:
				init = new AST.Init(genOneRepAtomExpr(t), exp);
				attrLoc.put(init, t.location());
				return init;
		}
	}

	private AST.AtomExpr checkConst() {

		AST.AtomExpr exp = null;

		Token t = lexAn.peekToken();
		switch (next()) {
			case INTCONST:
				exp = new AST.AtomExpr(AST.AtomExpr.Type.INTCONST, t.lexeme());
				break;
			case STRINGCONST:
				exp = new AST.AtomExpr(AST.AtomExpr.Type.STRCONST, t.lexeme());
				break;
			case CHARCONST:
				exp = new AST.AtomExpr(AST.AtomExpr.Type.CHRCONST, t.lexeme());
				break;
			default:
				error();
				break;
		}

		attrLoc.put(exp, t.location());
		return exp;
	}

	private int getEndLine(List<? extends AST.Node> list) {
		if (list.isEmpty())
			return 0;
		Report.Locatable l = attrLoc.get(list.getLast());
		return l.location().endLine();
	}

	private int getEndColumn(List<? extends AST.Node> list) {
		if (list.isEmpty())
			return 0;
		Report.Locatable l = attrLoc.get(list.getLast());
		return l.location().endColumn();
	}

	private String error() {
		final Token t = lexAn.peekToken();
		throw new Report.Error(t.location() + "Syntax error. Unexpected symbol '" + t.lexeme() + "'.");
	}

	/*** TODO ***/

	// --- ZAGON ---

	/**
	 * Zagon sintaksnega analizatorja kot samostojnega programa.
	 * 
	 * @param cmdLineArgs Argumenti v ukazni vrstici.
	 */
	public static void main(final String[] cmdLineArgs) {
		System.out.println("This is PINS'25 compiler (syntax analysis):");

		try {
			if (cmdLineArgs.length == 0)
				throw new Report.Error("No source file specified in the command line.");
			if (cmdLineArgs.length > 1)
				Report.warning("Unused arguments in the command line.");

			try (SynAn synAn = new SynAn(cmdLineArgs[0])) {
				synAn.parse(new HashMap<AST.Node, Report.Locatable>());
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

}
