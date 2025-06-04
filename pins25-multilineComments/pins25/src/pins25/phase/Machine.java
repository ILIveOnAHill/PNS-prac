package pins25.phase;

import java.util.*;

import pins25.common.*;

/**
 * Skladovni stroj.
 * 
 * Naslovi 'sistemskih' funkcij:
 * <ol>
 * <li>{@code -1}: {@code fun exit(exitcode)}</li>
 * <li>{@code -2}: {@code fun getint()}</li>
 * <li>{@code -3}: {@code fun putint(intvalue)}</li>
 * <li>{@code -4}: {@code fun getstr(straddr)}</li>
 * <li>{@code -5}: {@code fun putstr(straddr)}</li>
 * <li>{@code -6}: {@code fun new(size)}</li>
 * <li>{@code -7}: {@code fun del(addr)}</li>
 * </ol>
 */
public class Machine {

	@SuppressWarnings({ "doclint:missing" })
	public Machine() {
		throw new Report.InternalError();
	}

	/** Ali se opravi testni izpis ukazov. */
	public static boolean debugInstrsList = true;

	/** Ali se opravi testni izpis vrednost oznak. */
	public static boolean debugLabelsList = true;

	/** Ali se opravi testni izpis dogajanja na skladu. */
	public static boolean debugStack = false;

	/**
	 * Izvajanje skladovnega stroja.
	 */
	public static class Executor {

		/** Seznam ukazov kode programa. */
		private final HashMap<Integer, PDM.CodeInstr> program = new HashMap<Integer, PDM.CodeInstr>();

		/** Pomnilnik (brez predstavitve ukazov. */
		private final HashMap<Integer, Byte> memory = new HashMap<Integer, Byte>();

		/** Preslikava imen oznak v fizicne naslove. */
		private final HashMap<String, Integer> labelToAddr = new HashMap<String, Integer>();

		/** Preslikava fizicnih naslovov v imena oznak. */
		private final HashMap<Integer, String> addrToLabel = new HashMap<Integer, String>();

		/** Velikost segmenta z ukazi kode programa. */
		private final int codeSegmentSize;

		/** Velikost segmenta s staticnimi spremenljivkami. */
		private final int dataSegmentSize;

		/** Preslikava naslova v lokacijo kode, ki je izvor vrednosti na naslovu. */
		final HashMap<Integer, String> debugLocs = new HashMap<Integer, String>();

		/** Preslikava naslova v pomen podatka, ki je shranjen na naslovu. */
		final HashMap<Integer, String> debugDscs = new HashMap<Integer, String>();

		{
			labelToAddr.put("exit", -1);
			addrToLabel.put(-1, "exit");
			labelToAddr.put("getint", -2);
			addrToLabel.put(-2, "getint");
			labelToAddr.put("putint", -3);
			addrToLabel.put(-3, "putint");
			labelToAddr.put("getstr", -4);
			addrToLabel.put(-4, "getstr");
			labelToAddr.put("putstr", -5);
			addrToLabel.put(-5, "putstr");
			labelToAddr.put("new", -6);
			addrToLabel.put(-6, "new");
			labelToAddr.put("del", -7);
			addrToLabel.put(-7, "del");
		}

		/** Programski stevec. */
		private int PC;

		/** Klicni kazalec. */
		private int FP;

		/** Skladovni kazalec. */
		private int SP;

		/** Kazalec na prvi prosti naslov na kopici. */
		private int HP;

		/**
		 * Shrani vrednost v pomnilnik.
		 * 
		 * @param addr       Pomnilniski naslov.
		 * @param value      Vrednost.
		 * @param debugInstr Lokacija dela izvorne kode, ki zahteva shranjevanje.
		 */
		private void memSAVE(int addr, int value, final PDM.INSTR debugInstr) {
			if (addr < codeSegmentSize)
				throw new Report.InternalError();
			if (debugStack && (debugInstr != null) && (debugInstr.debugLocation != null))
				debugLocs.put(addr, debugInstr.debugLocation.toString());
			for (int b = 0; b < 4; b++) {
				int val = ((value >> (b * 8)) & 0xFF);
				memory.put(addr, (byte) val);
				addr += 1;
			}
		}

		/**
		 * Prebere vrednost iz pomnilnika.
		 * 
		 * @param addr Pomnilniski naslov.
		 * @return Vrednost.
		 */
		private int memLOAD(int addr) {
			if (addr < codeSegmentSize)
				throw new Report.InternalError();
			int value = 0;
			for (int b = 0; b < 4; b++) {
				Byte val = memory.get(addr);
				addr += 1;
				if (val == null)
					val = 0;
				value = value | (((val < 0) ? ((int) val) + 256 : ((int) val)) << (b * 8));
			}
			return value;
		}

		/**
		 * Prenos nove vrednosti na sklad.
		 * 
		 * @param value      Vrednost.
		 * @param debugInstr Lokacija dela izvorne kode, ki prenos nove vrednosti na
		 *                   sklad.
		 */
		private void push(final int value, final PDM.INSTR debugInstr) {
			SP -= 4;
			memSAVE(SP, value, debugInstr);
		}

		/**
		 * Prevzem vrednost z vrha sklada.
		 * 
		 * @return Vrednost.
		 */
		private int pop() {
			if (debugStack)
				debugLocs.put(SP, null);
			final int value = memLOAD(SP);
			SP += 4;
			return value;
		}

		/**
		 * Ustvari nov skladovni stroj za podan program in ta program izvede.
		 * 
		 * @param codeSegment Seznam ukazov, ki predstavljajo kodo programa.
		 * @param dataSegment Seznam ukazov, ki predstavljajo podatke programa.
		 */
		public Executor(final List<PDM.CodeInstr> codeSegment, final List<PDM.DataInstr> dataSegment) {

			Scanner scanner = new Scanner(System.in);

			int memPtr = 0;

			if (debugLabelsList)
				System.out.println("\n\033[1mCODE LABELS:\033[0m");
			for (final PDM.CodeInstr instr : codeSegment) {
				switch (instr) {
					case PDM.LABEL i -> {
						labelToAddr.put(i.name, memPtr);
						addrToLabel.put(memPtr, i.name);
						if (debugLabelsList)
							System.out.printf("LABEL %s = %d\n", i.name, memPtr);
						memPtr -= 1;
					}
					case PDM.INIT i -> program.put(memPtr, i);
					case PDM.LOAD i -> program.put(memPtr, i);
					case PDM.SAVE i -> program.put(memPtr, i);
					case PDM.POPN i -> program.put(memPtr, i);
					case PDM.PUSH i -> {
						program.put(memPtr, i);
						memPtr += 4;
					}
					case PDM.NAME i -> {
						program.put(memPtr, i);
						memPtr += 4;
					}
					case PDM.REGN i -> program.put(memPtr, i);
					case PDM.OPER i -> program.put(memPtr, i);
					case PDM.UJMP i -> program.put(memPtr, i);
					case PDM.CJMP i -> program.put(memPtr, i);
					case PDM.CALL i -> program.put(memPtr, i);
					case PDM.RETN i -> program.put(memPtr, i);
					default -> throw new Report.InternalError();
				}
				memPtr += 1;
			}
			codeSegmentSize = memPtr;

			if (debugLabelsList)
				System.out.println("\n\033[1mDATA LABELS:\033[0m");
			for (final PDM.DataInstr instr : dataSegment) {
				switch (instr) {
					case PDM.LABEL i -> {
						labelToAddr.put(i.name, memPtr);
						addrToLabel.put(memPtr, i.name);
						if (debugLabelsList)
							System.out.printf("LABEL %s = %d\n", i.name, memPtr);
					}
					case PDM.SIZE i -> {
						memPtr += i.size;
					}
					case PDM.DATA i -> {
						memSAVE(memPtr, i.intc, i);
						memPtr += 4;
					}
					default -> throw new Report.InternalError();
				}
			}
			dataSegmentSize = memPtr - codeSegmentSize;

			PC = 0;
			FP = 0x10000;
			SP = 0x10000;
			HP = codeSegmentSize + dataSegmentSize;

			push(-1, null);
			FP = SP + 0;
			push(-1, null);
			SP = SP + 0;

			System.out.printf("\n");
			loop: while (true) {

				if (debugStack) {
					for (int stackAddr = 0x10000 - 4; stackAddr >= SP; stackAddr -= 4) {
						final String debugLoc = debugLocs.get(stackAddr);
						System.out.printf("%15s ", debugLoc == null ? "" : debugLoc);
						if (stackAddr == FP)
							System.out.printf("FP => ");
						else if (stackAddr == SP)
							System.out.printf("SP => ");
						else
							System.out.printf("      ");
						System.out.printf("%6d: %12d", stackAddr, memLOAD(stackAddr));
						final String debugDsc = debugDscs.get(stackAddr);
						System.out.printf(" %s", debugDsc == null ? "" : debugDsc);
						System.out.printf("\n");
					}
					System.out.printf("\n");
				}

				final PDM.CodeInstr instr = program.get(PC);
				if (debugStack) {
					System.out.printf("\033[1m%15s %5d: %s\033[0m\n\n",
							((PDM.INSTR) instr).debugLocation == null ? "" : ((PDM.INSTR) instr).debugLocation, PC,
							instr.toString());
				}

				switch (instr) {
					case PDM.INIT i: {
						int initAddr = pop();
						int dstAddr = pop();
						final int numInits = memLOAD(initAddr);
						initAddr += 4;
						for (int nInit = 0; nInit < numInits; nInit++) {
							int num = memLOAD(initAddr);
							initAddr += 4;
							int len = memLOAD(initAddr);
							initAddr += 4;
							for (int n = 0; n < num; n++) {
								for (int l = 0; l < len; l++) {
									memSAVE(dstAddr, memLOAD(initAddr + 4 * l), i);
									dstAddr += 4;
								}
							}
							initAddr += 4 * len;
						}
						PC += i.size();
						break;
					}
					case PDM.LOAD i: {
						int addr = pop();
						int value = memLOAD(addr);
						push(value, i);
						PC += i.size();
						break;
					}
					case PDM.SAVE i: {
						final int addr = pop();
						final int value = pop();
						memSAVE(addr, value, i);
						PC += i.size();
						break;
					}
					case PDM.POPN i: {
						int n = pop();
						if (n < 0) {
							while (n < 0) {
								push(0, i);
								n += 4;
							}
						} else {
							while (n > 0) {
								pop();
								n -= 4;
							}
						}
						PC += i.size();
						break;
					}
					case PDM.PUSH i: {
						push(i.intc, i);
						PC += i.size();
						break;
					}
					case PDM.NAME i: {
						push(labelToAddr.get(i.name), i);
						PC += i.size();
						break;
					}
					case PDM.REGN i: {
						final int value = switch (i.regn) {
							case PC -> PC;
							case FP -> FP;
							case SP -> SP;
							default -> throw new Report.InternalError();
						};
						push(value, i);
						PC += i.size();
						break;
					}
					case PDM.OPER i: {
						switch (i.oper) {
							case NOT:
							case NEG: {
								final int expr = pop();
								final int result = switch (i.oper) {
									case NOT -> (expr == 0) ? 1 : 0;
									case NEG -> -expr;
									default -> throw new Report.InternalError();
								};
								push(result, i);
								break;
							}
							case OR:
							case AND:
							case EQU:
							case NEQ:
							case GTH:
							case LTH:
							case GEQ:
							case LEQ:
							case ADD:
							case SUB:
							case MUL:
							case DIV:
							case MOD: {
								final int snd = pop();
								final int fst = pop();
								int result = switch (i.oper) {
									case OR -> (fst != 0) || (snd != 0) ? 1 : 0;
									case AND -> (fst != 0) && (snd != 0) ? 1 : 0;
									case EQU -> fst == snd ? 1 : 0;
									case NEQ -> fst != snd ? 1 : 0;
									case GTH -> fst > snd ? 1 : 0;
									case LTH -> fst < snd ? 1 : 0;
									case GEQ -> fst >= snd ? 1 : 0;
									case LEQ -> fst <= snd ? 1 : 0;
									case ADD -> fst + snd;
									case SUB -> fst - snd;
									case MUL -> fst * snd;
									case DIV -> fst / snd;
									case MOD -> fst % snd;
									default -> throw new Report.InternalError();
								};
								push(result, i);
								break;
							}
							default:
								throw new Report.InternalError();
						}
						PC += i.size();
						break;
					}
					case PDM.UJMP i: {
						PC = pop();
						break;
					}
					case PDM.CJMP i: {
						final int elsePC = pop();
						final int thenPC = pop();
						final int cond = pop();
						PC = (cond != 0) ? thenPC : elsePC;
						break;
					}
					case PDM.CALL i: {
						final int newPC = pop();
						if (newPC < 0) {
							switch (newPC) {
								case -1: { // exit(exitcode)
									pop(); // SL
									final int exitCode = pop();
									pop();
									pop();
									System.out.printf("EXIT CODE (SP=%d): %d\n", SP, exitCode);
									break loop;
								}
								case -2: { // getint()
									pop(); // SL
									final int intValue = scanner.nextInt();
									push(intValue, null); // result
									PC += i.size();
									break;
								}
								case -3: { // putint(intvalue)
									pop(); // SL
									final int intValue = pop();
									System.out.printf("%d", intValue);
									push(1, null); // result
									PC += i.size();
									break;
								}
								case -4: { // getstr(straddr)
									pop(); // SL
									int strAddr = pop();
									final String strValue = scanner.nextLine();
									for (int c = 0; c < strValue.length(); c++) {
										memSAVE(strAddr, strValue.charAt(c), null);
										strAddr += 4;
									}
									memSAVE(strAddr, 0, null);
									push(1, null); // result
									PC += i.size();
									break;
								}
								case -5: { // putstr(straddr)
									pop(); // SL
									int strAddr = pop();
									while (true) {
										int c = memLOAD(strAddr);
										if (c == 0)
											break;
										System.out.printf("%c", c);
										strAddr += 4;
									}
									push(1, null); // result
									PC += i.size();
									break;
								}
								case -6: { // new(size)
									pop(); // SL
									final int size = pop();
									final int addr = HP;
									for (int a = addr; a < addr + size; a++)
										memory.put(a, (byte) 0);
									HP += size;
									push(addr, null); // result
									PC += i.size();
									break;
								}
								case -7: { // del(addr)
									pop(); // SL
									pop(); // addr
									push(1, null); // result
									PC += i.size();
									break;
								}
								default:
									throw new Report.InternalError();
							}
						} else {
							if (debugStack) {
								debugDscs.put(SP, "... SL");
								debugDscs.put(SP - 4,
										"... FP *** " + (i.debugFrame == null ? "" : i.debugFrame.name) + " ***");
								debugDscs.put(SP - 8, "... RA ");
								if (i.debugFrame != null) {
									if (i.debugFrame.debugPars != null)
										for (final Mem.RelAccess relAccess : i.debugFrame.debugPars)
											if (relAccess.debugName != null)
												debugDscs.put(SP + relAccess.offset, "... par: " + relAccess.debugName);
									if (i.debugFrame.debugVars != null)
										for (final Mem.RelAccess relAccess : i.debugFrame.debugVars)
											if (relAccess.debugName != null) {
												if (relAccess.size == 4)
													debugDscs.put(SP + relAccess.offset,
															"... var: " + relAccess.debugName);
												else {
													for (int s = 0; s < relAccess.size; s += 4)
														debugDscs.put(SP + relAccess.offset + s,
																"... var: " + relAccess.debugName + "[" + (s / 4)
																		+ "]");
												}
											}
								}
							}
							push(FP, i);
							push(PC + i.size(), i);
							FP = SP + 8;
							PC = newPC;
						}
						break;
					}
					case PDM.RETN i: {
						if (debugStack) {
							debugDscs.put(FP, null);
							debugDscs.put(FP - 4, null);
							debugDscs.put(FP - 8, null);
							if (i.debugFrame != null) {
								if (i.debugFrame.debugPars != null)
									for (final Mem.RelAccess relAccess : i.debugFrame.debugPars)
										if (relAccess.debugName != null)
											debugDscs.put(FP + relAccess.offset, null);
								if (i.debugFrame.debugVars != null)
									for (final Mem.RelAccess relAccess : i.debugFrame.debugVars)
										if (relAccess.debugName != null) {
											if (relAccess.size == 4)
												debugDscs.put(FP + relAccess.offset, null);
											else {
												for (int s = 0; s < relAccess.size; s += 4)
													debugDscs.put(FP + relAccess.offset + s, null);
											}
										}
							}
						}
						int parsSize = pop();
						final int result = pop();
						PC = memLOAD(FP - 8);
						while (SP != FP) {
							pop();
						}
						// SP = FP;
						FP = memLOAD(FP - 4);
						parsSize += 4;
						while (parsSize > 0) {
							pop();
							parsSize -= 4;
						}
						push(result, i);
						break;
					}
					default:
						throw new Report.InternalError();
				}
			}

			scanner.close();
		}

	}

	// --- ZAGON ---

	/**
	 * Zagon izracuna pomnilniske predstavitve kot samostojnega programa.
	 * 
	 * @param cmdLineArgs Argumenti v ukazni vrstici.
	 */
	public static void main(final String[] cmdLineArgs) {
		System.out.println("This is PINS'25 compiler (pushdown machine):");

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

				final List<PDM.CodeInstr> codeSegment = (new CodeGen.CodeSegmentGenerator(codegenAttrAST))
						.codeSegment();
				final List<PDM.DataInstr> dataSegment = (new CodeGen.DataSegmentGenerator(codegenAttrAST))
						.dataSegment();

				if (debugInstrsList) {
					int addr = 0;
					{
						System.out.println("\n\033[1mCODE SEGMENT:\033[0m");
						for (final PDM.CodeInstr instr : codeSegment) {
							System.out.printf("%8d [%s] %s\n", addr, instr.size(),
									(instr instanceof PDM.LABEL ? "" : "  ") + instr.toString());
							addr += instr.size();
						}
					}
					{
						System.out.println("\n\033[1mDATA SEGMENT:\033[0m");
						for (final PDM.DataInstr instr : dataSegment) {
							System.out.printf("%8d [%s] %s\n", addr, (instr instanceof PDM.SIZE) ? " " : instr.size(),
									(instr instanceof PDM.LABEL ? "" : "  ") + instr.toString());
							addr += instr.size();
						}
					}
				}

				// ustvari nov stroj in izvede program:
				new Executor(codeSegment, dataSegment);
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