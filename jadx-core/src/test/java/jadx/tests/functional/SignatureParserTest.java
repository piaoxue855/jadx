package jadx.tests.functional;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import jadx.core.dex.instructions.args.ArgType;
import jadx.core.dex.instructions.args.ArgType.WildcardBound;
import jadx.core.dex.nodes.GenericInfo;
import jadx.core.dex.nodes.parser.SignatureParser;

import static jadx.core.dex.instructions.args.ArgType.INT;
import static jadx.core.dex.instructions.args.ArgType.OBJECT;
import static jadx.core.dex.instructions.args.ArgType.array;
import static jadx.core.dex.instructions.args.ArgType.generic;
import static jadx.core.dex.instructions.args.ArgType.genericInner;
import static jadx.core.dex.instructions.args.ArgType.genericType;
import static jadx.core.dex.instructions.args.ArgType.object;
import static jadx.core.dex.instructions.args.ArgType.wildcard;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

class SignatureParserTest {

	@Test
	public void testSimpleTypes() {
		checkType("", null);
		checkType("I", INT);
		checkType("[I", array(INT));
		checkType("Ljava/lang/Object;", OBJECT);
		checkType("[Ljava/lang/Object;", array(OBJECT));
		checkType("[[I", array(array(INT)));
	}

	private static void checkType(String str, ArgType type) {
		assertThat(new SignatureParser(str).consumeType(), is(type));
	}

	@Test
	public void testGenerics() {
		checkType("TD;", genericType("D"));
		checkType("La<TV;Lb;>;", generic("La;", genericType("V"), object("b")));
		checkType("La<Lb<Lc;>;>;", generic("La;", generic("Lb;", object("Lc;"))));
		checkType("La/b/C<Ld/E<Lf/G;>;>;", generic("La/b/C;", generic("Ld/E;", object("Lf/G;"))));
		checkType("La<TD;>.c;", genericInner(generic("La;", genericType("D")), "c", null));
		checkType("La<TD;>.c/d;", genericInner(generic("La;", genericType("D")), "c.d", null));
		checkType("La<Lb;>.c<TV;>;", genericInner(generic("La;", object("Lb;")), "c", new ArgType[] { genericType("V") }));
	}

	@Test
	public void testInnerGeneric() {
		String signature = "La<TV;>.LinkedHashIterator<Lb$c<Ls;TV;>;>;";
		String objectStr = new SignatureParser(signature).consumeType().getObject();
		assertThat(objectStr, is("a$LinkedHashIterator"));
	}

	@Test
	public void testWildcards() {
		checkWildcards("*", wildcard());
		checkWildcards("+Lb;", wildcard(object("b"), WildcardBound.EXTENDS));
		checkWildcards("-Lb;", wildcard(object("b"), WildcardBound.SUPER));
		checkWildcards("+TV;", wildcard(genericType("V"), WildcardBound.EXTENDS));
		checkWildcards("-TV;", wildcard(genericType("V"), WildcardBound.SUPER));

		checkWildcards("**", wildcard(), wildcard());
		checkWildcards("*Lb;", wildcard(), object("b"));
		checkWildcards("*TV;", wildcard(), genericType("V"));
		checkWildcards("TV;*", genericType("V"), wildcard());
		checkWildcards("Lb;*", object("b"), wildcard());

		checkWildcards("***", wildcard(), wildcard(), wildcard());
		checkWildcards("*Lb;*", wildcard(), object("b"), wildcard());
	}

	private static void checkWildcards(String w, ArgType... types) {
		ArgType parsedType = new SignatureParser("La<" + w + ">;").consumeType();
		ArgType expectedType = generic("La;", types);
		assertThat(parsedType, is(expectedType));
	}

	@Test
	public void testGenericMap() {
		checkGenerics("");
		checkGenerics("<T:Ljava/lang/Object;>", "T", emptyList());
		checkGenerics("<K:Ljava/lang/Object;LongType:Ljava/lang/Object;>", "K", emptyList(), "LongType", emptyList());
		checkGenerics("<ResultT:Ljava/lang/Exception;:Ljava/lang/Object;>", "ResultT", singletonList(object("java.lang.Exception")));
	}

	@SuppressWarnings("unchecked")
	private static void checkGenerics(String g, Object... objs) {
		List<GenericInfo> genericsList = new SignatureParser(g).consumeGenericMap();
		List<GenericInfo> expectedList = new ArrayList<>();
		for (int i = 0; i < objs.length; i += 2) {
			ArgType generic = genericType((String) objs[i]);
			List<ArgType> list = (List<ArgType>) objs[i + 1];
			expectedList.add(new GenericInfo(generic, list));
		}
		assertThat(genericsList, is(expectedList));
	}

	@Test
	public void testMethodArgs() {
		List<ArgType> argTypes = new SignatureParser("(Ljava/util/List<*>;)V").consumeMethodArgs();

		assertThat(argTypes, hasSize(1));
		assertThat(argTypes.get(0), is(generic("Ljava/util/List;", wildcard())));
	}

	@Test
	public void testMethodArgs2() {
		List<ArgType> argTypes = new SignatureParser("(La/b/C<TT;>.d/E;)V").consumeMethodArgs();

		assertThat(argTypes, hasSize(1));
		ArgType argType = argTypes.get(0);
		assertThat(argType.getObject().indexOf('/'), is(-1));
		assertThat(argType, is(genericInner(generic("La/b/C;", genericType("T")), "d.E", null)));
	}

	@Test
	public void testBadGenericMap() {
		List<GenericInfo> list = new SignatureParser("<A:Ljava/lang/Object;B").consumeGenericMap();
		assertThat(list, hasSize(0));
	}
}
