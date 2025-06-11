package pins25.phase;

import java.util.*;
import pins25.common.*;
import pins25.common.AST.*;
import pins25.common.AST.BinExpr.Oper;
import pins25.common.Mem.*;
import pins25.common.PDM.*;
import pins25.common.PDM.REGN.Reg;
import pins25.common.Report.Locatable;

/**
 * Generiranje kode.
 */
public class CodeGen {

	@SuppressWarnings({ "doclint:missing" })
	public CodeGen() {
		throw new Report.InternalError();
	}

	/**
	 * Abstraktno sintaksno drevo z dodanimi atributi izracuna pomnilniske
	 * predstavitve.
	 * 
	 * Atributi:
	 * <ol>
	 * <li>({@link Abstr}) lokacija kode, ki pripada posameznemu vozliscu;</li>
	 * <li>({@link SemAn}) definicija uporabljenega imena;</li>
	 * <li>({@link SemAn}) ali je dani izraz levi izraz;</li>
	 * <li>({@link Memory}) klicni zapis funkcije;</li>
	 * <li>({@link Memory}) dostop do parametra;</li>
	 * <li>({@link Memory}) dostop do spremenljivke;</li>
	 * <li>({@link CodeGen}) seznam ukazov, ki predstavljajo kodo programa;</li>
	 * <li>({@link CodeGen}) seznam ukazov, ki predstavljajo podatke programa.</li>
	 * </ol>
	 */
	public static class AttrAST extends Memory.AttrAST {

		/** Atribut: seznam ukazov, ki predstavljajo kodo programa. */
		public final Map<AST.Node, List<PDM.CodeInstr>> attrCode;

		/** Atribut: seznam ukazov, ki predstavljajo podatke programa. */
		public final Map<AST.Node, List<PDM.DataInstr>> attrData;

		/**
		 * Ustvari novo abstraktno sintaksno drevo z dodanimi atributi generiranja kode.
		 * 
		 * @param attrAST  Abstraktno sintaksno drevo z dodanimi atributi pomnilniske
		 *                 predstavitve.
		 * @param attrCode Attribut: seznam ukazov, ki predstavljajo kodo programa.
		 * @param attrData Attribut: seznam ukazov, ki predstavljajo podatke programa.
		 */
		public AttrAST(final Memory.AttrAST attrAST, final Map<AST.Node, List<PDM.CodeInstr>> attrCode,
				final Map<AST.Node, List<PDM.DataInstr>> attrData) {
			super(attrAST);
			this.attrCode = attrCode;
			this.attrData = attrData;
		}

		/**
		 * Ustvari novo abstraktno sintaksno drevo z dodanimi atributi generiranja kode.
		 * 
		 * @param attrAST Abstraktno sintaksno drevo z dodanimi atributi generiranja
		 *                kode.
		 */
		public AttrAST(final AttrAST attrAST) {
			super(attrAST);
			this.attrCode = attrAST.attrCode;
			this.attrData = attrAST.attrData;
		}

		@Override
		public String head(final AST.Node node, final boolean highlighted) {
			final StringBuffer head = new StringBuffer();
			head.append(super.head(node, false));
			return head.toString();
		}

		@Override
		public void desc(final int indent, final AST.Node node, final boolean highlighted) {
			super.desc(indent, node, false);
			System.out.print(highlighted ? "\033[31m" : "");
			if (attrCode.get(node) != null) {
				List<PDM.CodeInstr> instrs = attrCode.get(node);
				if (instrs != null) {
					if (indent > 0)
						System.out.printf("%" + indent + "c", ' ');
					System.out.printf("--- Code: ---\n");
					for (final PDM.CodeInstr instr : instrs) {
						if (indent > 0)
							System.out.printf("%" + indent + "c", ' ');
						System.out.println((instr instanceof PDM.LABEL ? "" : "  ") + instr.toString());
					}
				}
			}
			if (attrData.get(node) != null) {
				List<PDM.DataInstr> instrs = attrData.get(node);
				if (instrs != null) {
					if (indent > 0)
						System.out.printf("%" + indent + "c", ' ');
					System.out.printf("--- Data: ---\n");
					for (final PDM.DataInstr instr : instrs) {
						if (indent > 0)
							System.out.printf("%" + indent + "c", ' ');
						System.out.println((instr instanceof PDM.LABEL ? "" : "  ") + instr.toString());
					}
				}
			}
			System.out.print(highlighted ? "\033[30m" : "");
			return;
		}

	}

	/**
	 * Izracuna kodo programa
	 * 
	 * @param memoryAttrAST Abstraktno sintaksno drevo z dodanimi atributi izracuna
	 *                      pomnilniske predstavitve.
	 * @return Abstraktno sintaksno drevo z dodanimi atributi izracuna pomnilniske
	 *         predstavitve.
	 */
	public static AttrAST generate(final Memory.AttrAST memoryAttrAST) {
		AttrAST attrAST = new AttrAST(memoryAttrAST, new HashMap<AST.Node, List<PDM.CodeInstr>>(),
				new HashMap<AST.Node, List<PDM.DataInstr>>());
		(new CodeGenerator(attrAST)).generate();
		return attrAST;
	}

	/**
	 * Generiranje kode v abstraktnem sintaksnem drevesu.
	 */
	private static class CodeGenerator {

		/**
		 * Abstraktno sintaksno drevo z dodanimi atributi izracuna pomnilniske
		 * predstavitve.
		 */
		private final AttrAST attrAST;

		/** Stevec anonimnih label. */
		// private int labelCounter = 0;

		/**
		 * Ustvari nov generator kode v abstraktnem sintaksnem drevesu.
		 * 
		 * @param attrAST Abstraktno sintaksno drevo z dodanimi atributi izracuna
		 *                pomnilniske predstavitve.
		 */
		public CodeGenerator(final AttrAST attrAST) {
			this.attrAST = attrAST;
		}

		/**
		 * Sprozi generiranje kode v abstraktnem sintaksnem drevesu.
		 * 
		 * @return Abstraktno sintaksno drevo z dodanimi atributi izracuna pomnilniske
		 *         predstavitve.
		 */
		public AttrAST generate() {
			attrAST.ast.accept(new Generator(), null);
			return new AttrAST(attrAST, Collections.unmodifiableMap(attrAST.attrCode),
					Collections.unmodifiableMap(attrAST.attrData));
		}

		/** Obiskovalec, ki generira kodo v abstraktnem sintaksnem drevesu. */
		private class Generator implements AST.FullVisitor<List<PDM.CodeInstr>, Mem.Frame> {

			private HashMap<Mem.Frame, AST.Node> frameReturnNode = new HashMap<>();

			@SuppressWarnings({ "doclint:missing" })
			public Generator() {
			}

			@Override
			public List<CodeInstr> visit(Nodes<? extends Node> nodes, Frame arg) {
				var code = new ArrayList<CodeInstr>();

				for (var node : nodes) {
					code.addAll(node.accept(this, arg));
				}

				return code;
			}

			public List<CodeInstr> varOrParAddr(VarExpr varExpr, Frame frame) {
				var code = new ArrayList<CodeInstr>();
				var loc = attrAST.attrLoc.get(varExpr);
				var def = attrAST.attrDef.get(varExpr);

				var access = (def instanceof VarDef ? attrAST.attrVarAccess : attrAST.attrParAccess).get(def);

				switch (access) {
					case RelAccess ra -> {
						code.add(new PDM.REGN(Reg.FP, loc));

						for (var d = ra.depth; d < frame.depth; d++) {
							code.add(new PDM.LOAD(loc));
						}
						code.add(new PDM.PUSH(ra.offset, loc));
						code.add(new PDM.OPER(PDM.OPER.Oper.ADD, loc));
					}
					case AbsAccess _ -> {
						var defLoc = attrAST.attrLoc.get(def);
						code.add(new PDM.NAME(generateLabel("global", varExpr.name, defLoc), loc));
					}
					default -> never();
				}

				return code;
			}

			@Override
			public List<CodeInstr> visit(final AST.FunDef funDef, Frame frame) {
				if (funDef.stmts.size() == 0)
					return Collections.emptyList();

				var code = new ArrayList<PDM.CodeInstr>();
				Locatable loc = attrAST.attrLoc.get(funDef);
				var curFrame = attrAST.attrFrame.get(funDef);

				AST.Stmt lastStmt = funDef.stmts.getAll().getLast();
				bp: while (true) {
					switch (lastStmt) {
						case AST.ExprStmt e -> {
							frameReturnNode.put(curFrame, e);
							break bp;
						}

						case AST.LetStmt letStmt -> {
							if (letStmt.stmts.size() != 0)
								lastStmt = letStmt.stmts.getAll().getLast();
							else
								never();
						}
						default -> never();
					}
				}

				String label = curFrame.depth == 1 ? funDef.name : generateLabel("func", funDef.name, loc);
				code.add(new PDM.LABEL(label, loc));

				frame = attrAST.attrFrame.get(funDef);

				if (curFrame.varsSize > 8) {
					code.add(new PDM.PUSH(-curFrame.varsSize + 8, loc));
					code.add(new PDM.POPN(loc));
				}

				code.addAll(funDef.stmts.accept(this, curFrame));

				code.add(new PDM.PUSH(funDef.pars.size() * 4, loc));
				code.add(new PDM.RETN(curFrame, loc));

				attrAST.attrCode.put(funDef, code);
				return code;
			}

			@Override
			public List<PDM.CodeInstr> visit(final AST.VarDef varDef, final Mem.Frame frame) {
				var code = new ArrayList<CodeInstr>();
				var data = new ArrayList<PDM.DataInstr>();
				Locatable loc = attrAST.attrLoc.get(varDef);
				String initLabel = generateLabel(varDef.name, loc);
				var access = attrAST.attrVarAccess.get(varDef);

				data.add(new PDM.LABEL(initLabel, loc));
				data.addAll(access.inits.stream().map(x -> new PDM.DATA(x, loc)).toList());

				switch (access) {
					case RelAccess ra -> {
						code.add(new PDM.REGN(Reg.FP, loc));
						code.add(new PDM.PUSH(ra.offset, loc));
						code.add(new PDM.OPER(PDM.OPER.Oper.ADD, loc));
					}
					default -> {
						String varLabel = generateLabel("global", varDef.name, loc);

						data.add(new LABEL(varLabel, loc));
						data.add(new PDM.SIZE(access.size, loc));

						code.add(new PDM.NAME(varLabel, loc));

					}
				}

				code.add(new PDM.NAME(initLabel, loc));
				code.add(new PDM.INIT(loc));

				attrAST.attrCode.put(varDef, code);
				attrAST.attrData.put(varDef, data);

				return code;
			}

			@Override
			public List<PDM.CodeInstr> visit(final AST.WhileStmt whileStmt, final Mem.Frame frame) {
				var code = new ArrayList<PDM.CodeInstr>();

				Locatable loc = attrAST.attrLoc.get(whileStmt);

				String wLabel = generateLabel("while:condition", loc);
				String wBodyLabel = generateLabel("while:body", loc);
				String wEndLabel = generateLabel("while:end", loc);

				code.add(new PDM.LABEL(wLabel, loc));
				code.addAll(whileStmt.cond.accept(this, frame));

				code.add(new PDM.NAME(wBodyLabel, loc));
				code.add(new PDM.NAME(wEndLabel, loc));
				code.add(new PDM.CJMP(loc));

				code.add(new PDM.LABEL(wBodyLabel, loc));
				code.addAll(whileStmt.stmts.accept(this, frame));

				code.add(new PDM.NAME(wLabel, loc));
				code.add(new PDM.UJMP(loc));

				code.add(new PDM.LABEL(wEndLabel, loc));

				attrAST.attrCode.put(whileStmt, code);
				return code;
			}

			@Override
			public List<PDM.CodeInstr> visit(final AST.IfStmt ifStmt, final Mem.Frame frame) {
				var code = new ArrayList<PDM.CodeInstr>();

				Locatable loc = attrAST.attrLoc.get(ifStmt);
				String thenLabel = generateLabel("if:then", loc);
				String ifElseLabel = generateLabel("if:else", loc);
				String ifEndLabel = generateLabel("if:end", loc);

				code.addAll(ifStmt.cond.accept(this, frame));

				if (ifStmt.elseStmts.size() == 0) {
					code.add(new PDM.NAME(thenLabel, loc));
					code.add(new PDM.NAME(ifEndLabel, loc));
					code.add(new PDM.CJMP(loc));

					code.add(new PDM.LABEL(thenLabel, loc));
					code.addAll(ifStmt.thenStmts.accept(this, frame));

					code.add(new PDM.LABEL(thenLabel, loc));
				} else {
					code.add(new PDM.NAME(thenLabel, loc));
					code.add(new PDM.NAME(ifElseLabel, loc));
					code.add(new PDM.CJMP(loc));

					code.add(new PDM.LABEL(thenLabel, loc));
					code.addAll(ifStmt.thenStmts.accept(this, frame));

					code.add(new PDM.NAME(ifEndLabel, loc));
					code.add(new PDM.UJMP(loc));

					code.add(new PDM.LABEL(ifElseLabel, loc));
					code.addAll(ifStmt.elseStmts.accept(this, frame));

					code.add(new PDM.LABEL(ifEndLabel, loc));
				}

				attrAST.attrCode.put(ifStmt, code);
				return code;
			}

			@Override
			public List<PDM.CodeInstr> visit(final AST.LetStmt letStmt, final Mem.Frame frame) {
				var code = new ArrayList<PDM.CodeInstr>();

				for (AST.Def def : letStmt.defs) {
					if (def instanceof VarDef) {
						code.addAll(def.accept(this, frame));
					} else {
						def.accept(this, frame);
					}
				}

				code.addAll(letStmt.stmts.accept(this, frame));

				attrAST.attrCode.put(letStmt, code);
				return code;
			}

			@Override
			public List<PDM.CodeInstr> visit(final AST.ExprStmt exprStmt, final Mem.Frame frame) {
				var code = new ArrayList<PDM.CodeInstr>();
				var loc = attrAST.attrLoc.get(exprStmt);

				code.addAll(exprStmt.expr.accept(this, frame));

				var returnNode = frameReturnNode.get(frame);
				if (exprStmt != returnNode) {
					code.add(new PDM.PUSH(1, loc));
					code.add(new PDM.POPN(loc));
				}
				attrAST.attrCode.put(exprStmt, code);
				return code;
			}

			@Override
			public List<PDM.CodeInstr> visit(final AST.AssignStmt assignStmt, final Mem.Frame frame) {
				var code = new Vector<PDM.CodeInstr>();
				Locatable loc = attrAST.attrLoc.get(assignStmt);

				code.addAll(assignStmt.srcExpr.accept(this, frame));

				if (assignStmt.dstExpr instanceof AST.VarExpr ve) {
					code.addAll(varOrParAddr(ve, frame));
				} else if (assignStmt.dstExpr instanceof AST.UnExpr ue) {
					if (ue.oper != AST.UnExpr.Oper.VALUEAT) {
						throw new InternalError();
					}
					code.addAll(ue.expr.accept(this, frame));
				} else {
					throw new InternalError();
				}

				code.add(new PDM.SAVE(loc));

				attrAST.attrCode.put(assignStmt, code);
				return code;
			}

			@Override
			public List<PDM.CodeInstr> visit(final AST.CallExpr callExpr, final Mem.Frame frame) {
				var code = new Vector<PDM.CodeInstr>();
				Locatable loc = attrAST.attrLoc.get(callExpr);
				var def = attrAST.attrDef.get(callExpr);
				var defFrame = attrAST.attrFrame.get(def);
				Locatable defLoc = attrAST.attrLoc.get(def);

				if (def == null || !(def instanceof FunDef)) {
					throw new Report.Error(loc, "Cannot call non-function");
				}

				for (AST.Expr arg : callExpr.args.getAll().reversed()) {
					code.addAll(arg.accept(this, frame));
				}

				String callLabel = defFrame.depth == 1 ? def.name : generateLabel("func", def.name, defLoc);

				code.add(new PDM.REGN(Reg.FP, loc));
				for (int d = frame.depth; d >= defFrame.depth; d--) {
					code.add(new PDM.LOAD(loc));
				}

				code.add(new PDM.NAME(callLabel, loc));
				code.add(new PDM.CALL(defFrame, loc));

				attrAST.attrCode.put(callExpr, code);
				return code;
			}

			@Override
			public List<PDM.CodeInstr> visit(final AST.UnExpr unExpr, final Mem.Frame frame) {
				var code = new ArrayList<PDM.CodeInstr>();
				Locatable loc = attrAST.attrLoc.get(unExpr);
				switch (unExpr.oper) {
					case AST.UnExpr.Oper.ADD -> {
						code.addAll(unExpr.expr.accept(this, frame));
					}

					case AST.UnExpr.Oper.NOT -> {
						code.addAll(unExpr.expr.accept(this, frame));
						code.add(new PDM.OPER(PDM.OPER.Oper.NOT, loc));
					}

					case AST.UnExpr.Oper.SUB -> {
						code.addAll(unExpr.expr.accept(this, frame));
						code.add(new PDM.OPER(PDM.OPER.Oper.NEG, loc));
					}

					case AST.UnExpr.Oper.VALUEAT -> {
						code.addAll(unExpr.expr.accept(this, frame));
						code.add(new PDM.LOAD(loc));
					}
					case AST.UnExpr.Oper.MEMADDR -> {
						switch (unExpr.expr) {
							case VarExpr ve -> {
								code.addAll(varOrParAddr(ve, frame));
							}

							default -> {
								throw new Report.Error(loc, String.format("Cannot take address of %s", unExpr.expr));
							}
						}
					}
				}

				attrAST.attrCode.put(unExpr, code);
				return code;
			}

			@Override
			public List<PDM.CodeInstr> visit(final AST.BinExpr binExpr, final Mem.Frame frame) {
				var code = new ArrayList<PDM.CodeInstr>();

				var loc = attrAST.attrLoc.get(binExpr);

				String endLabel = generateLabel("bin:expr:end", loc);
				String processLabel = generateLabel("bin:expr:2nd", loc);
				String idkLabel = generateLabel("bin:expr:skip", loc);

				code.addAll(binExpr.fstExpr.accept(this, frame));

				if (binExpr.oper == Oper.OR) {
					code.add(new PDM.NAME(endLabel, loc));
					code.add(new PDM.NAME(processLabel, loc));
					code.add(new PDM.CJMP(loc));

					code.add(new PDM.LABEL(processLabel, loc));
					code.add(new PDM.PUSH(0, loc));

					code.addAll(binExpr.sndExpr.accept(this, frame));
					code.add(new PDM.OPER(getBinOper(binExpr.oper), loc));

					code.add(new PDM.NAME(idkLabel, loc));
					code.add(new PDM.UJMP(loc));

					code.add(new PDM.LABEL(endLabel, loc));
					code.add(new PDM.PUSH(1, loc));

					code.add(new PDM.LABEL(idkLabel, loc));

				} else if (binExpr.oper == Oper.AND) {
					code.add(new PDM.NAME(processLabel, loc));
					code.add(new PDM.NAME(endLabel, loc));
					code.add(new PDM.CJMP(loc));

					code.add(new PDM.LABEL(processLabel, loc));
					code.add(new PDM.PUSH(1, loc));

					code.addAll(binExpr.sndExpr.accept(this, frame));
					code.add(new PDM.OPER(getBinOper(binExpr.oper), loc));

					code.add(new PDM.NAME(idkLabel, loc));
					code.add(new PDM.UJMP(loc));

					code.add(new PDM.LABEL(endLabel, loc));
					code.add(new PDM.PUSH(0, loc));

					code.add(new PDM.LABEL(idkLabel, loc));
				} else {
					code.addAll(binExpr.sndExpr.accept(this, frame));
					code.add(new PDM.OPER(getBinOper(binExpr.oper), loc));
				}

				attrAST.attrCode.put(binExpr, code);
				return code;
			}

			@Override
			public List<PDM.CodeInstr> visit(final AST.VarExpr varExpr, final Mem.Frame frame) {
				var code = new ArrayList<PDM.CodeInstr>();
				Locatable loc = attrAST.attrLoc.get(varExpr);

				code.addAll(varOrParAddr(varExpr, frame));
				code.add(new PDM.LOAD(loc));

				attrAST.attrCode.put(varExpr, code);
				return code;
			}

			@Override
			public List<PDM.CodeInstr> visit(final AST.AtomExpr atomExpr, final Mem.Frame frame) {
				var code = new ArrayList<PDM.CodeInstr>();
				Locatable loc = attrAST.attrLoc.get(atomExpr);
				switch (atomExpr.type) {
					case INTCONST:
						code.add(new PDM.PUSH(Memory.decodeIntConst(atomExpr, loc), loc));
						break;
					case CHRCONST:
						code.add(new PUSH(Memory.decodeChrConst(atomExpr, loc), loc));
						break;
					case STRCONST:
						var data = new ArrayList<PDM.DataInstr>();
						String label = generateLabel("strconst", loc);
						data.add(new LABEL(label, loc));
						for (int s : Memory.decodeStrConst(atomExpr, loc)) {
							data.add(new DATA(s, loc));
						}
						code.add(new PDM.NAME(label, loc));
						attrAST.attrData.put(atomExpr, data);
						break;
				}

				attrAST.attrCode.put(atomExpr, code);
				return code;
			}

			@Override
			public List<CodeInstr> visit(Init __, Frame ___) {
				return Collections.emptyList();
			}

			@Override
			public List<CodeInstr> visit(ParDef __, Frame ___) {
				return Collections.emptyList();
			}

			// Privatne metode

			private String generateLabel(String def, String name, Locatable loc) {
				return String.format("%s:%s@%d:%d", def, name, loc.location().begLine(),
						loc.location().begColumn());
			}

			private String generateLabel(String def, Locatable loc) {
				return String.format("%s@%d:%d", def, loc.location().begLine(),
						loc.location().begColumn());
			}

			private PDM.OPER.Oper getBinOper(AST.BinExpr.Oper oper) {
				return switch (oper) {
					case OR -> PDM.OPER.Oper.OR;
					case AND -> PDM.OPER.Oper.AND;
					case EQU -> PDM.OPER.Oper.EQU;
					case NEQ -> PDM.OPER.Oper.NEQ;
					case LTH -> PDM.OPER.Oper.LTH;
					case GTH -> PDM.OPER.Oper.GTH;
					case LEQ -> PDM.OPER.Oper.LEQ;
					case GEQ -> PDM.OPER.Oper.GEQ;
					case ADD -> PDM.OPER.Oper.ADD;
					case SUB -> PDM.OPER.Oper.SUB;
					case MUL -> PDM.OPER.Oper.MUL;
					case DIV -> PDM.OPER.Oper.DIV;
					case MOD -> PDM.OPER.Oper.MOD;
				};
			}

			private void never() {
				throw new Error("Unreachable reached");
			}

			/* TODO */

		}

	}

	/**
	 * Generator seznama ukazov, ki predstavljajo kodo programa.
	 */
	public static class CodeSegmentGenerator {

		/**
		 * Abstraktno sintaksno drevo z dodanimi atributi izracuna pomnilniske
		 * predstavitve.
		 */
		private final AttrAST attrAST;

		/** Seznam ukazov za inicializacijo staticnih spremenljivk. */
		private final Vector<PDM.CodeInstr> codeInitSegment = new Vector<PDM.CodeInstr>();

		/** Seznam ukazov funkcij. */
		private final Vector<PDM.CodeInstr> codeFunsSegment = new Vector<PDM.CodeInstr>();

		/** Klicni zapis funkcije {@code main}. */
		private Mem.Frame main = null;

		/**
		 * Ustvari nov generator seznama ukazov, ki predstavljajo kodo programa.
		 *
		 * @param attrAST Abstraktno sintaksno drevo z dodanimi atributi izracuna
		 *                pomnilniske predstavitve.
		 */
		public CodeSegmentGenerator(final AttrAST attrAST) {
			this.attrAST = attrAST;
		}

		/**
		 * Izracuna seznam ukazov, ki predstavljajo kodo programa.
		 * 
		 * @return Seznam ukazov, ki predstavljajo kodo programa.
		 */
		public List<PDM.CodeInstr> codeSegment() {
			attrAST.ast.accept(new Generator(), null);
			codeInitSegment.addLast(new PDM.PUSH(0, null));
			codeInitSegment.addLast(new PDM.NAME("main", null));
			codeInitSegment.addLast(new PDM.CALL(main, null));
			codeInitSegment.addLast(new PDM.PUSH(0, null));
			codeInitSegment.addLast(new PDM.NAME("exit", null));
			codeInitSegment.addLast(new PDM.CALL(null, null));
			final Vector<PDM.CodeInstr> codeSegment = new Vector<PDM.CodeInstr>();
			codeSegment.addAll(codeInitSegment);
			codeSegment.addAll(codeFunsSegment);
			return Collections.unmodifiableList(codeSegment);
		}

		/**
		 * Obiskovalec, ki izracuna seznam ukazov, ki predstavljajo kodo programa.
		 */
		private class Generator implements AST.FullVisitor<Object, Object> {

			@SuppressWarnings({ "doclint:missing" })
			public Generator() {
			}

			@Override
			public Object visit(final AST.FunDef funDef, final Object arg) {
				if (funDef.stmts.size() == 0)
					return null;
				List<PDM.CodeInstr> code = attrAST.attrCode.get(funDef);
				codeFunsSegment.addAll(code);
				funDef.pars.accept(this, arg);
				funDef.stmts.accept(this, arg);
				switch (funDef.name) {
					case "main" -> main = attrAST.attrFrame.get(funDef);
				}
				return null;
			}

			@Override
			public Object visit(final AST.VarDef varDef, final Object arg) {
				switch (attrAST.attrVarAccess.get(varDef)) {
					case Mem.AbsAccess _: {
						List<PDM.CodeInstr> code = attrAST.attrCode.get(varDef);
						codeInitSegment.addAll(code);
						break;
					}
					case Mem.RelAccess _: {
						break;
					}
					default:
						throw new Report.InternalError();
				}
				return null;
			}

		}

	}

	/**
	 * Generator seznama ukazov, ki predstavljajo podatke programa.
	 */
	public static class DataSegmentGenerator {

		/**
		 * Abstraktno sintaksno drevo z dodanimi atributi izracuna pomnilniske
		 * predstavitve.
		 */
		private final AttrAST attrAST;

		/** Seznam ukazov, ki predstavljajo podatke programa. */
		private final Vector<PDM.DataInstr> dataSegment = new Vector<PDM.DataInstr>();

		/**
		 * Ustvari nov generator seznama ukazov, ki predstavljajo podatke programa.
		 *
		 * @param attrAST Abstraktno sintaksno drevo z dodanimi atributi izracuna
		 *                pomnilniske predstavitve.
		 */
		public DataSegmentGenerator(final AttrAST attrAST) {
			this.attrAST = attrAST;
		}

		/**
		 * Izracuna seznam ukazov, ki predstavljajo podatke programa.
		 * 
		 * @return Seznam ukazov, ki predstavljajo podatke programa.
		 */
		public List<PDM.DataInstr> dataSegment() {
			attrAST.ast.accept(new Generator(), null);
			return Collections.unmodifiableList(dataSegment);
		}

		/**
		 * Obiskovalec, ki izracuna seznam ukazov, ki predstavljajo podatke programa.
		 */
		private class Generator implements AST.FullVisitor<Object, Object> {

			@SuppressWarnings({ "doclint:missing" })
			public Generator() {
			}

			@Override
			public Object visit(final AST.VarDef varDef, final Object arg) {
				List<PDM.DataInstr> data = attrAST.attrData.get(varDef);
				if (data != null)
					dataSegment.addAll(data);
				varDef.inits.accept(this, arg);
				return null;
			}

			@Override
			public Object visit(final AST.AtomExpr atomExpr, final Object arg) {
				List<PDM.DataInstr> data = attrAST.attrData.get(atomExpr);
				if (data != null)
					dataSegment.addAll(data);
				return null;
			}

		}

	}

	// --- ZAGON ---

	/**
	 * Zagon izracuna pomnilniske predstavitve kot samostojnega programa.
	 * 
	 * @param cmdLineArgs Argumenti v ukazni vrstici.
	 */
	public static void main(final String[] cmdLineArgs) {
		System.out.println("This is PINS'25 compiler (code generation):");

		try {
			if (cmdLineArgs.length == 0)
				throw new Report.Error("No source file specified in the command line.");
			if (cmdLineArgs.length > 1)
				Report.warning("Unused arguments in the command line.");

			try (SynAn synAn = new SynAn(cmdLineArgs[0])) {
				// abstraktna sintaksa:
				final Abstr.AttrAST abstrAttrAST = Abstr.constructAST(synAn);
				// semanticna analiza:
				final SemAn.AttrAST semanAttrAST = SemAn.analyze(abstrAttrAST);
				// pomnilniska predstavitev:
				final Memory.AttrAST memoryAttrAST = Memory.organize(semanAttrAST);
				// generiranje kode:
				final CodeGen.AttrAST codegenAttrAST = CodeGen.generate(memoryAttrAST);

				(new AST.Logger(codegenAttrAST)).log();
				{
					int addr = 0;
					final List<PDM.CodeInstr> codeSegment = (new CodeSegmentGenerator(codegenAttrAST)).codeSegment();
					{
						System.out.println("\n\033[1mCODE SEGMENT:\033[0m");
						for (final PDM.CodeInstr instr : codeSegment) {
							System.out.printf("%8d [%s] %s\n", addr, instr.size(),
									(instr instanceof PDM.LABEL ? "" : "  ") + instr.toString());
							addr += instr.size();
						}
					}
					final List<PDM.DataInstr> dataSegment = (new DataSegmentGenerator(codegenAttrAST)).dataSegment();
					{
						System.out.println("\n\033[1mDATA SEGMENT:\033[0m");
						for (final PDM.DataInstr instr : dataSegment) {
							System.out.printf("%8d [%s] %s\n", addr, (instr instanceof PDM.SIZE) ? " " : instr.size(),
									(instr instanceof PDM.LABEL ? "" : "  ") + instr.toString());
							addr += instr.size();
						}
					}
					System.out.println();
				}
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