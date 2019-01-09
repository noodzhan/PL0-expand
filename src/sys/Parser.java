package sys;

/**
 *　　语法分析器。这是PL/0分析器中最重要的部分，在语法分析的过程中穿插着语法错误检查和目标代码生成。
 */
public class Parser {
	private Scanner lex;					// 对词法分析器的引用
	private Table table;					// 对符号表的引用
	private Interpreter interp;				// 对目标代码生成器的引用
	
	private final int symnum = Symbol.values().length;
	
	// 表示声明开始的符号集合、表示语句开始的符号集合、表示因子开始的符号集合
	// 实际上这就是声明、语句和因子的FIRST集合
	private SymSet declbegsys, statbegsys, facbegsys;
	
	/**
	 * 当前符号，由nextsym()读入
	 * @see #nextSym()
	 */
	private Symbol sym;
	
	/**
	 * 当前作用域的堆栈帧大小，或者说数据大小（data size）
	 */
	private int dx = 0;
	
	/**
	 * 构造并初始化语法分析器，这里包含了C语言版本中init()函数的一部分代码
	 * @param l 编译器的词法分析器
	 * @param t 编译器的符号表
	 * @param i 编译器的目标代码生成器
	 */
	public Parser(Scanner l, Table t, Interpreter i) {
		lex = l;
		table = t;
		interp = i;
		
		// 设置声明开始符号集
		declbegsys = new SymSet(symnum);
		declbegsys.set(Symbol.constsym);
		declbegsys.set(Symbol.varsym);
		declbegsys.set(Symbol.procsym);

		// 设置语句开始符号集
		statbegsys = new SymSet(symnum);
		statbegsys.set(Symbol.beginsym);
		statbegsys.set(Symbol.callsym);
		statbegsys.set(Symbol.ifsym);
		statbegsys.set(Symbol.whilesym);
		statbegsys.set(Symbol.readsym);			// thanks to elu
		statbegsys.set(Symbol.writesym);
		// 设置因子开始符号集
		facbegsys = new SymSet(symnum);
		facbegsys.set(Symbol.ident);
		facbegsys.set(Symbol.number);
		facbegsys.set(Symbol.lparen);//左括号
		facbegsys.set(Symbol.falsesym);//false
		facbegsys.set(Symbol.truesym);//true
		facbegsys.set(Symbol.notsym);

	}
	
	/**
	 * 启动语法分析过程，此前必须先调用一次nextsym()
	 * @see #nextSym()
	 */
	public void parse() {
		//System.out.println("symum="+symnum+"\n");
		SymSet nxtlev = new SymSet(symnum);//symnum是Symbol这个保留字的大小
		nxtlev.or(declbegsys);//相当于赋值，有1得1，有0为0
		nxtlev.or(statbegsys);//这两步其实就是求声明开始符号集和因子开始符号集的并集
		nxtlev.set(Symbol.period);//period就是程序的结束标志
		parseBlock(0, nxtlev);
		
		if (sym != Symbol.period)
			Err.report(9);
	}
	
	/**
	 * 获得下一个语法符号，这里只是简单调用一下getsym()
	 */
	public void nextSym() {
		lex.getsym();//进行词法分析，获取一个单词
		sym =lex.sym;
	}
	
	/**
	 * 测试当前符号是否合法
	 * 
	 * @param s1 我们需要的符号
	 * @param s2 如果不是我们需要的，则需要一个补救用的集合
	 * @param errcode 错误号
	 */
	void test(SymSet s1, SymSet s2, int errcode) {
		// 在某一部分（如一条语句，一个表达式）将要结束时时我们希望下一个符号属于某集合
		//（该部分的后跟符号），test负责这项检测，并且负责当检测不通过时的补救措施，程
		// 序在需要检测时指定当前需要的符号集合和补救用的集合（如之前未完成部分的后跟符
		// 号），以及检测不通过时的错误号。
		if (!s1.get(sym)) {
			Err.report(errcode);
			// 当检测不通过时，不停获取符号，直到它属于需要的集合或补救的集合
			while (!s1.get(sym) && !s2.get(sym))
				nextSym();
		}
	}
	
	/**
	 * 分析<分程序>
	 * 
	 * @param lev 当前分程序所在层
	 * @param fsys 当前模块后跟符号集
	 */
	public void parseBlock(int lev, SymSet fsys) {
		// <分程序> := [<常量说明部分>][<变量说明部分>][<过程说明部分>]<语句>
		
		int dx0, tx0, cx0;				// 保留初始dx，tx和cx
		SymSet nxtlev = new SymSet(symnum);
		
		dx0 = dx;						// 记录本层之前的数据量（以便恢复）
		dx = 3;
		tx0 = table.tx;					// 记录本层名字的初始位置（以便恢复）
		table.get(table.tx).adr = interp.cx;//interp.cx是虚拟机生成p-code的指针
											//****.adr 是Item的地址，就是符号的属性
		interp.gen(Fct.JMP, 0, 0);
		
		if (lev > PL0.levmax)
			Err.report(32);
		
		// 分析<说明部分>
		do {
			// <常量说明部分>
			if (sym == Symbol.constsym) {
				nextSym();
				// the original do...while(sym == ident) is problematic, thanks to calculous
				// do
				parseConstDeclaration(lev);
				while (sym == Symbol.comma) {  //，
					nextSym();
					parseConstDeclaration(lev);
				}
				
				if (sym == Symbol.semicolon)  //；
					nextSym();
				else
					Err.report(5);				// 漏掉了逗号或者分号
				// } while (sym == ident);
			}
			
			// <变量说明部分>
			if (sym == Symbol.varsym) {
				nextSym();
				// the original do...while(sym == ident) is problematic, thanks to calculous
				// do {
				parseVarDeclaration(fsys,lev);
				while (sym == Symbol.comma)
				{
					nextSym();
					parseVarDeclaration(fsys,lev);
				}
				
				if (sym == Symbol.semicolon)
					nextSym();
				else
					Err.report(5);				// 漏掉了逗号或者分号
				// } while (sym == ident);
			}
			
			// <过程说明部分>
			while (sym == Symbol.procsym) {
				nextSym();
				if (sym == Symbol.ident) {
					table.enter(Objekt.procedure, lev, dx);
					nextSym();
				} else { 
					Err.report(4);				// procedure后应为标识符
				}

				if (sym == Symbol.semicolon)
					nextSym();
				else
					Err.report(5);				// 漏掉了分号
				
				nxtlev = (SymSet) fsys.clone();
				nxtlev.set(Symbol.semicolon);
				parseBlock(lev+1, nxtlev);
				
				if (sym == Symbol.semicolon) {
					nextSym();
					nxtlev = (SymSet) statbegsys.clone();
					nxtlev.set(Symbol.ident);
					nxtlev.set(Symbol.procsym);
					test(nxtlev, fsys, 6);
				} else { 
					Err.report(5);				// 漏掉了分号
				}
			}
			
			nxtlev = (SymSet) statbegsys.clone(); 
			nxtlev.set(Symbol.ident);
			test(nxtlev, declbegsys, 7); //  声明某一类型不能大于2个
		} while (declbegsys.get(sym));		// 直到没有声明符号
		
		// 开始生成当前过程代码
		Table.Item item = table.get(tx0);
		interp.code[item.adr].a = interp.cx;
		item.adr = interp.cx;					// 当前过程代码地址
		item.size = dx;							// 声明部分中每增加一条声明都会给dx增加1，
												// 声明部分已经结束，dx就是当前过程的堆栈帧大小
		cx0 = interp.cx;
		interp.gen(Fct.INT, 0, dx);			// 生成分配内存代码
		
		table.debugTable(tx0);
			
		// 分析<语句>
		nxtlev = (SymSet) fsys.clone();		// 每个后跟符号集和都包含上层后跟符号集和，以便补救
		nxtlev.set(Symbol.semicolon);		// 语句后跟符号为分号或end
		nxtlev.set(Symbol.endsym);
		parseStatement(nxtlev, lev);
		interp.gen(Fct.OPR, 0, 0);		// 每个过程出口都要使用的释放数据段指令
		
		nxtlev = new SymSet(symnum);	// 分程序没有补救集合
		test(fsys, nxtlev, 8);				// 检测后跟符号正确性
		
		interp.listcode(cx0);
		
		dx = dx0;							// 恢复堆栈帧计数器
		table.tx = tx0;						// 回复名字表位置
	}

	/**
	 * 分析<常量说明部分>
	 * @param lev 当前所在的层次
	 */
	void parseConstDeclaration(int lev) {
		if (sym == Symbol.ident) {
			nextSym();
			if (sym == Symbol.eql || sym == Symbol.becomes) {
				if (sym == Symbol.becomes) 
					Err.report(1);			// 把 = 写成了 :=
				nextSym();
				if (sym == Symbol.number) {
					table.enter(Objekt.constant, lev, dx);
					nextSym();
				} else {
					Err.report(2);			// 常量说明 = 后应是数字
				}
			} else {
				Err.report(3);				// 常量说明标识后应是 =
			}
		} else {
			Err.report(4);					// const 后应是标识符
		}
	}

	/**
	 * 分析<变量说明部分>
	 * @param lev 当前层次
	 *//*
	void parseVarDeclaration(int lev) {
		if (sym == Symbol.ident) {
			// 填写名字表并改变堆栈帧计数器
			table.enter(Objekt.variable, lev, dx);
			dx ++;
			nextSym();
		} else {
			Err.report(4);					// var 后应是标识
		}
	}
	*/
	
	/**
	 * 分析<变量Integer>
	 * 
	 */
	void parseIntegerDeclartion(int lev) {
		if(sym==Symbol.integersym) {
			nextSym();
			if(sym==Symbol.ident) {
				table.enter(Objekt.variable, lev, dx,0);
				dx ++;
				nextSym();
			}else {
				
				//报错   后面应该是标识符
				Err.report(4);	
			}
		}
	}
	
	
	/**
	 * 分析<变量bool>
	 * @param lev 当前层次
	 */
	void parseBooleanDeclartion(int lev) {
		if (sym == Symbol.booleansym) {
			nextSym();
			if(sym==Symbol.ident) {
				// 填写名字表并改变堆栈帧计数器
				table.enter(Objekt.variable, lev, dx,1);
				dx ++;
				nextSym();
			}else {
				//报错   后面应该是 标识符
				Err.report(4);	
			}
		} 
	}
	
	
	/**
	 * 分析<其他变量>
	 * @param lev 当前层数
	 */
	void parseOtherVarDeclartion(int lev) {
		
		if (sym == Symbol.ident) {
			// 填写名字表并改变堆栈帧计数器
			table.enter(Objekt.variable, lev, dx,2);
			dx ++;
			nextSym();
		} else {
			Err.report(4);					// var 后应是标识
		}
	}
	
	
	
	
	/**
	 * 分析var变量说明部分
	 */
	void parseVarDeclaration(SymSet fsys,int lev) {
		SymSet nxtlev;
		switch(sym) {
		case ident:    //空
			parseOtherVarDeclartion(lev);
			break;
		case booleansym:
			parseBooleanDeclartion(lev);
			break;
		case integersym:
			parseIntegerDeclartion(lev);
			break;
		default:
			
			//应该加入报错处理
			
			break;
				
		}
		
	}
	


	/**
	 * 分析<语句>
	 * @param fsys 后跟符号集
	 * @param lev 当前层次
	 */
	void parseStatement(SymSet fsys, int lev) {
		SymSet nxtlev;
		// Wirth 的 PL/0 编译器使用一系列的if...else...来处理
		// 但是你的助教认为下面的写法能够更加清楚地看出这个函数的处理逻辑
		switch (sym) {
		case ident:
			parseAssignStatement(fsys, lev);
			break;
		case readsym:
			parseReadStatement(fsys, lev);
			break;
		case writesym:
			parseWriteStatement(fsys, lev);
			break;
		case callsym:
			parseCallStatement(fsys, lev);
			break;
		case ifsym:
			parseIfStatement(fsys, lev);
			break;
		case beginsym:
			parseBeginStatement(fsys, lev);
			break;
		case whilesym:
			parseWhileStatement(fsys, lev);
			break;
		case forsym:
			parseForStatement(fsys,lev);
		case elsesym:
			parseElseStatement(fsys,lev);
		default:
			nxtlev = new SymSet(symnum);
			test(fsys, nxtlev, 19);
			break;
		}
	}
	
	
	
	
	
	private void parseElseStatement(SymSet fsys, int lev) {
		// TODO Auto-generated method stub
		
	}

	private void parseForStatement(SymSet fsys, int lev) {
		// TODO Auto-generated method stub
		
	}

	/**
	 * 分析<当型循环语句>
	 * @param fsys 后跟符号集
	 * @param lev 当前层次
	 */
	private void parseWhileStatement(SymSet fsys, int lev) {
		int cx1, cx2;
		SymSet nxtlev;
		
		cx1 = interp.cx;						// 保存判断条件操作的位置
		nextSym();
		nxtlev = (SymSet) fsys.clone();
		nxtlev.set(Symbol.dosym);				// 后跟符号为do
		parseCondition(nxtlev, lev);			// 分析<条件>
		cx2 = interp.cx;						// 保存循环体的结束的下一个位置
		interp.gen(Fct.JPC, 0, 0);				// 生成条件跳转，但跳出循环的地址未知
		if (sym == Symbol.dosym)
			nextSym();
		else
			Err.report(18);						// 缺少do
		parseStatement(fsys, lev);				// 分析<语句>
		interp.gen(Fct.JMP, 0, cx1);			// 回头重新判断条件
		interp.code[cx2].a = interp.cx;			// 反填跳出循环的地址，与<条件语句>类似
	}

	/**
	 * 分析<复合语句>
	 * @param fsys 后跟符号集
	 * @param lev 当前层次
	 */
	private void parseBeginStatement(SymSet fsys, int lev) {
		SymSet nxtlev;
		
		nextSym();
		nxtlev = (SymSet) fsys.clone();
		nxtlev.set(Symbol.semicolon);
		nxtlev.set(Symbol.endsym);
		parseStatement(nxtlev, lev);
		// 循环分析{; <语句>}，直到下一个符号不是语句开始符号或收到end
		while (statbegsys.get(sym) || sym == Symbol.semicolon) {
			if (sym == Symbol.semicolon)
				nextSym();
			else
				Err.report(10);					// 缺少分号
			parseStatement(nxtlev, lev);
		}
		if (sym == Symbol.endsym)
			nextSym();
		else
			Err.report(17);						// 缺少end或分号
	}

	/**
	 * 分析<条件语句>
	 * @param fsys 后跟符号集
	 * @param lev 当前层次
	 */
	private void parseIfStatement(SymSet fsys, int lev) {
		int cx1;
		SymSet nxtlev;
		
		nextSym();
		nxtlev = (SymSet) fsys.clone();
		nxtlev.set(Symbol.thensym);				// 后跟符号为then或do ???
		nxtlev.set(Symbol.dosym);
		parseCondition(nxtlev, lev);			// 分析<条件>
		if (sym == Symbol.thensym)
			nextSym();
		else
			Err.report(16);						// 缺少then
		
		
		cx1 = interp.cx;						// 保存当前指令地址
		interp.gen(Fct.JPC, 0, 0);				// 生成条件跳转指令，跳转地址未知，暂时写0
		parseStatement(fsys, lev);				// 处理then后的语句
		
		nextSym();
		
		if(sym==Symbol.elsesym) {
			//System.out.println("进入");
			int cx2=interp.cx;
			nextSym();
			interp.gen(Fct.JMP,0,0);//执行then后面的就结束
			
			interp.code[cx1].a = interp.cx;  //不满足要求跳转到else  
			parseStatement(fsys,lev);
			interp.code[cx2].a = interp.cx;  
		}else {
			interp.code[cx1].a = interp.cx;	//没有else 执行原本的操作
			//经statement处理后，cx为then后语句执行完的位置，它正是前面未定的跳转地址
		}

		
	}
	

	/**
	 * 分析<过程调用语句>
	 * @param fsys 后跟符号集
	 * @param lev 当前层次
	 */
	private void parseCallStatement(SymSet fsys, int lev) {
		int i;
		nextSym();
		if (sym == Symbol.ident) {
			i = table.position(lex.id);
			if (i == 0) {
				Err.report(11);					// 过程未找到
			} else {
				Table.Item item = table.get(i);
				if (item.kind == Objekt.procedure)
					interp.gen(Fct.CAL, lev - item.level, item.adr);
				else
					Err.report(15);				// call后标识符应为过程
			}
			nextSym();
		} else {
			Err.report(14);						// call后应为标识符
		}
	}

	/**
	 * 分析<写语句>
	 * @param fsys 后跟符号集
	 * @param lev 当前层次
	 */
	private void parseWriteStatement(SymSet fsys, int lev) {
		SymSet nxtlev;

		nextSym();
		if (sym == Symbol.lparen) {
			do {
				nextSym();
				nxtlev = (SymSet) fsys.clone();
				nxtlev.set(Symbol.rparen);
				nxtlev.set(Symbol.comma);
				parseExpression(nxtlev, lev);
				interp.gen(Fct.OPR, 0, 14);
			} while (sym == Symbol.comma);
			
			if (sym == Symbol.rparen)
				nextSym();
			else
				Err.report(33);				// write()中应为完整表达式
		}
		interp.gen(Fct.OPR, 0, 15);
	}

	/**
	 * 分析<读语句>
	 * @param fsys 后跟符号集
	 * @param lev 当前层次
	 */
	private void parseReadStatement(SymSet fsys, int lev) {
		int i;
		
		nextSym();
		if (sym == Symbol.lparen) {
			do {
				nextSym();
				if (sym == Symbol.ident)
					i = table.position(lex.id);
				else
					i = 0;
				
				if (i == 0) {
					Err.report(35);			// read()中应是声明过的变量名
				} else {
					Table.Item item = table.get(i);
					if (item.kind != Objekt.variable) {
						Err.report(32);		// read()中的标识符不是变量, thanks to amd
					} else {
						interp.gen(Fct.OPR, 0, 16);
						interp.gen(Fct.STO, lev-item.level, item.adr);
					}
				}
				
				nextSym();
			} while (sym == Symbol.comma);
		} else {
			Err.report(34);					// 格式错误，应是左括号
		}
		
		if (sym == Symbol.rparen) {
			nextSym();
		} else {
			Err.report(33);					// 格式错误，应是右括号
			while (!fsys.get(sym))
				nextSym();
		}
	}

	/**
	 * 分析<赋值语句>
	 * @param fsys 后跟符号集
	 * @param lev 当前层次
	 */
	private void parseAssignStatement(SymSet fsys, int lev) {
		int i;
		SymSet nxtlev;
		
		i = table.position(lex.id);
		if (i > 0) {
			Table.Item item = table.get(i);
			if (item.kind == Objekt.variable) {
				nextSym();
				if (sym == Symbol.becomes)
					nextSym();
				else
					Err.report(13);					// 没有检测到赋值符号
				nxtlev = (SymSet) fsys.clone();
				parseExpression(nxtlev, lev);
				// parseExpression将产生一系列指令，但最终结果将会保存在栈顶，执行sto命令完成赋值
				interp.gen(Fct.STO, lev - item.level, item.adr);//将栈顶的元素写到内存中去
			} else {
				Err.report(12);						// 赋值语句格式错误
			}
		} else {
			Err.report(11);							// 变量未找到
		}
	}

	/**
	 * 分析<表达式>
	 * @param fsys 后跟符号集
	 * @param lev 当前层次
	 */
	private void parseExpression(SymSet fsys, int lev) {
		Symbol addop;
		SymSet nxtlev;
		
		// 分析[+|-]<项>
		if (sym == Symbol.plus || sym == Symbol.minus) {
			addop = sym;
			nextSym();
			nxtlev = (SymSet) fsys.clone();
			nxtlev.set(Symbol.plus);   //<项> 后面的跟的因子
			nxtlev.set(Symbol.minus);  
			parseTerm(nxtlev, lev);  
			
			if (addop == Symbol.minus)
				interp.gen(Fct.OPR, 0, 1);
		} else {
			nxtlev = (SymSet) fsys.clone();
			nxtlev.set(Symbol.plus);
			nxtlev.set(Symbol.minus);
			parseTerm(nxtlev, lev);
		}
		
		// 分析{<加法运算符><项>}
		while (sym == Symbol.plus || sym == Symbol.minus) {
			addop = sym;
			nextSym();
			nxtlev = (SymSet) fsys.clone();
			nxtlev.set(Symbol.plus);
			nxtlev.set(Symbol.minus);
			parseTerm(nxtlev, lev);
			if (addop == Symbol.plus)
				interp.gen(Fct.OPR, 0, 2);
			else
				interp.gen(Fct.OPR, 0, 3);
		}
		
		
		// 分析{or <项>}
		while (sym == Symbol.orsym) {
			addop = sym;
			nextSym();
			nxtlev = (SymSet) fsys.clone();
			nxtlev.set(Symbol.orsym);
			parseTerm(nxtlev, lev);
			
			//生成目标代码  实现or
			interp.gen(Fct.OPR, 0, 17);

		}		
		
	}

	/**
	 * 分析<项>
	 * @param fsys 后跟符号集
	 * @param lev 当前层次
	 */
	private void parseTerm(SymSet fsys, int lev) {
		Symbol mulop;
		SymSet nxtlev;

		// 分析<因子>
		nxtlev = (SymSet) fsys.clone();
		nxtlev.set(Symbol.times);
		nxtlev.set(Symbol.slash);
		parseFactor(nxtlev, lev);
		
		// 分析{<乘法运算符><因子>}  
		while (sym == Symbol.times || sym == Symbol.slash) {
			mulop = sym;
			nextSym();
			parseFactor(nxtlev, lev);
			if (mulop == Symbol.times)
				interp.gen(Fct.OPR, 0, 4);
			else if(mulop == Symbol.slash){
				interp.gen(Fct.OPR, 0, 5);
			}
		}
		
		//分析{<and><因子>}
		while (sym == Symbol.andsym) {
			mulop = sym;
			nextSym();
			parseFactor(nxtlev, lev);
			interp.gen(Fct.OPR,0,18);//生成and
		}
		
		
		
	}

	/**
	 * 分析<因子>
	 * @param fsys 后跟符号集
	 * @param lev 当前层次
	 */
	private void parseFactor(SymSet fsys, int lev) {
		SymSet nxtlev;
		
		test(facbegsys, fsys, 24);			// 检测因子的开始符号
		// the original while... is problematic: var1(var2+var3)
		// thanks to macross
		// while(inset(sym, facbegsys))
		if (facbegsys.get(sym)) {
			if (sym == Symbol.ident) {			// 因子为常量或变量
				int i = table.position(lex.id);
				if (i > 0) {
					Table.Item item = table.get(i);
					switch (item.kind) {
					case constant:			// 名字为常量
						interp.gen(Fct.LIT, 0, item.val);
						break;
					case variable:			// 名字为变量
						interp.gen(Fct.LOD, lev - item.level, item.adr);
						break;
					case procedure:			// 名字为过程
						Err.report(21);				// 不能为过程
						break;
					}
				} else {
					Err.report(11);					// 标识符未声明
				}
				nextSym();
			} else if (sym == Symbol.number) {	// 因子为数 
				int num = lex.num;
				if (num > PL0.amax) {
					Err.report(31);
					num = 0;
				}
				interp.gen(Fct.LIT, 0, num);
				nextSym();
			} else if (sym == Symbol.lparen) {	// 因子为表达式
				nextSym();
				nxtlev = (SymSet) fsys.clone();
				nxtlev.set(Symbol.rparen);
				parseExpression(nxtlev, lev);
				if (sym == Symbol.rparen)
					nextSym();
				else
					Err.report(22);					// 缺少右括号
			} else if(sym==Symbol.falsesym) {  //因子为布尔常量  false
				
				interp.gen(Fct.LIT, 0, 0);//生成目标代码
				nextSym();
			
			} else if(sym==Symbol.truesym) {
				
				interp.gen(Fct.LIT, 0, 1);
				nextSym();
			}else if(sym==Symbol.notsym) {    //
				
				nextSym();
				parseFactor(facbegsys,lev);
				interp.gen(Fct.OPR, 0, 20); 
				
			}
			 else {
				// 做补救措施
				test(fsys, facbegsys, 23);
			}
		}
	}

	/**
	 * 分析<条件>
	 * @param fsys 后跟符号集
	 * @param lev 当前层次
	 */
	private void parseCondition(SymSet fsys, int lev) {
		Symbol relop;
		SymSet nxtlev;
		
		if (sym == Symbol.oddsym) { //奇数
			// 分析 ODD<表达式>
			nextSym();
			parseExpression(fsys, lev);
			interp.gen(Fct.OPR, 0, 6);
		} else {
			
			// 分析<表达式><关系运算符><表达式>
			nxtlev = (SymSet) fsys.clone();
			nxtlev.set(Symbol.eql);
			nxtlev.set(Symbol.neq);
			nxtlev.set(Symbol.lss);
			nxtlev.set(Symbol.leq);
			nxtlev.set(Symbol.gtr);
			nxtlev.set(Symbol.geq);
			parseExpression(nxtlev, lev);
			//System.out.println("ex"+sym.toString());
			if (sym == Symbol.eql || sym == Symbol.neq 
					|| sym == Symbol.lss || sym == Symbol.leq
					|| sym == Symbol.gtr || sym == Symbol.geq) {
				relop = sym;
				nextSym();
				parseExpression(fsys, lev);
				switch (relop) {
				case eql:
					interp.gen(Fct.OPR, 0, 8);
					break;
				case neq:
					interp.gen(Fct.OPR, 0, 9);
					break;
				case lss:
					interp.gen(Fct.OPR, 0, 10);
					break;
				case geq:
					interp.gen(Fct.OPR, 0, 11);
					break;
				case gtr:
					interp.gen(Fct.OPR, 0, 12);
					break;
				case leq:
					interp.gen(Fct.OPR, 0, 13);
					break;
				}
			} else if(sym==Symbol.thensym) {
					
				
			}
			else  {
				Err.report(20);
			}
		}
	}
}
