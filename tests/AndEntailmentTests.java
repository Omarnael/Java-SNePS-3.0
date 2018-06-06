import java.lang.reflect.Field;
import java.util.LinkedList;

import junit.framework.TestCase;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import sneps.exceptions.DuplicatePropositionException;
import sneps.exceptions.NodeNotFoundInNetworkException;
import sneps.exceptions.NotAPropositionNodeException;
import sneps.network.Network;
import sneps.network.Node;
import sneps.network.PropositionNode;
import sneps.network.RuleNode;
import sneps.network.VariableNode;
import sneps.network.cables.DownCable;
import sneps.network.cables.DownCableSet;
import sneps.network.classes.CaseFrame;
import sneps.network.classes.Relation;
import sneps.network.classes.Semantic;
import sneps.network.classes.term.Open;
import sneps.network.classes.term.Term;
import sneps.network.classes.term.Variable;
import sneps.setClasses.ContextRuisSet;
import sneps.setClasses.FlagNodeSet;
import sneps.setClasses.NodeSet;
import sneps.setClasses.PropositionSet;
import sneps.snebr.Context;
import sneps.snebr.Controller;
import sneps.snip.Report;
import sneps.snip.classes.FlagNode;
import sneps.snip.classes.PTree;
import sneps.snip.classes.RuisHandler;
import sneps.snip.classes.RuleUseInfo;
import sneps.snip.matching.LinearSubstitutions;
import sneps.snip.rules.AndEntailment;


public class AndEntailmentTests extends TestCase{
	private static AndEntailment and;
	private static Node fido;
	private static Node var;
	private static Node dog;
	private static RuleUseInfo rui;
	private static Report report;

	@BeforeClass
 	public void setUp() throws Exception {
		var = new VariableNode(new Variable("X"));
		fido = Network.buildBaseNode("Fido", new Semantic("Member"));
		dog = Network.buildBaseNode("Dog", new Semantic("Class"));
		and.addAntecedent(var);
		and.addAntecedent(dog);
		and.addAntecedent(fido);

		LinearSubstitutions sub = new LinearSubstitutions();
		FlagNodeSet fns = new FlagNodeSet();
		PropositionSet support = new PropositionSet();

		support.add(dog.getId());
		FlagNode fn = new FlagNode(dog, support, 1);
		fns.insert(fn);

		support.clearSet();
		support.add(fido.getId());
		fn = new FlagNode(fido, support, 1);
		fns.insert(fn);

		rui = new RuleUseInfo(sub, 1, 0, fns);

		NodeSet c1 = new NodeSet();
		Relation rel = new Relation("Class", "type");
		c1.addNode(dog);
		LinkedList<DownCable> dc = new LinkedList<DownCable>();
		LinkedList<Relation> rels = new LinkedList<Relation>();
		rels.add(rel);
		dc.add(new DownCable(rel, c1));

		c1 = new NodeSet();
		rel = new Relation("Member", "type");
		c1.addNode(fido);
		dc = new LinkedList<DownCable>();
		rels = new LinkedList<Relation>();
		rels.add(rel);
		dc.add(new DownCable(rel, c1));

		c1 = new NodeSet();
		rel = new Relation("Var", "type");
		c1.addNode(var);
		dc = new LinkedList<DownCable>();
		rels = new LinkedList<Relation>();
		rels.add(rel);

		dc.add(new DownCable(rel, c1));
		DownCableSet dcs = new DownCableSet(dc, new CaseFrame("string", rels));
		support.add(dog.getId());
		support.add(fido.getId());
		support.add(var.getId());
		report = new Report(sub, support, true, "default");

		and = new AndEntailment(new Open("Wat", dcs));
	}

	@Test
	public void testApplyRuleHandler() {
		and.setKnownInstances(and.getNewInstances());
		and.getNewInstances().clear();

		and.applyRuleHandler(report, fido);
		if(and.getAntSize() <= 1)
			assertNotNull("AndEntailment: ApplyRuleHandler doesn't broadcast report",
					and.getNewInstances());
		else
			assertNull("AndEntailment: ApplyRuleHandler broacdcasts final report without waiting for enough positive antecedents reports",
					and.getNewInstances());


		and.setKnownInstances(and.getNewInstances());
		and.getNewInstances().clear();
		LinearSubstitutions sub = new LinearSubstitutions();
		FlagNodeSet fns = new FlagNodeSet();
		PropositionSet support = new PropositionSet();

		try {
			support.add(dog.getId());
		} catch (DuplicatePropositionException | NotAPropositionNodeException
				| NodeNotFoundInNetworkException e) {}

		FlagNode fn = new FlagNode(dog, support, 1);
		fns.insert(fn);
		report = new Report(sub, support, false, "default");

		and.applyRuleHandler(report, dog);
		if(and.getAntSize() <= 1)
			assertNull(
					"AndEntailment: ApplyRuleHandler broadcasts negative report",
					((PropositionNode)and).getNewInstances());
	}

	@Test
	public void testGetDownAntNodeSet() {
		NodeSet downAntNodeSet = and.getDownAntNodeSet();
		assertNotNull("AndEntailment: getDownAntNodeSet retuns null", downAntNodeSet);
		assertTrue("AndEntailment: getDownAntNodeSet retuns an empty NodeSet", !downAntNodeSet.isEmpty());
	}

	@Test
	public void testCreateRuisHandler() {
		Context contxt = (Context) Controller.getContextByName("default");
		and.createRuisHandler("default");
		RuisHandler handler = and.getContextRuiHandler(contxt);
		assertNotNull(
				"AndEntailment: CreateRuisHandler creats a null RuisHandler",
				handler);
		assertTrue(
				"AndEntailment: CreateRuisHandler doesn't create a PTree as a Handler",
				handler instanceof PTree);
	}

	@Test
	public void testAndEntailmentTerm() {
		Class<AndEntailment> aClass = AndEntailment.class;
		boolean thrown = false;
		try {
			aClass.getConstructor(new Class[] {
					Term.class });
		} catch (Exception e) {
			thrown = true;
		}
		assertFalse(
				"Missing constructor with Term parameter in AndEntailment class.",
				thrown);

		assertEquals(
				"AndEntailment class should extend RuleNode",
				RuleNode.class,
				AndEntailment.class.getSuperclass());

		AndEntailment e = and;
		Field f;
		try {
			f = e.getClass().getDeclaredField("contextRuisSet");

			f.setAccessible(true);
			f.set(e, new ContextRuisSet());

			assertNotNull(
					"The constructor of AndEntailment class should initialize inherited variables correctly by calling super.",
					f);
		} catch(Exception x){
			assertNull(x.getMessage(), x);
		}
	}

	@Test
	public void testAndEntailmentSemanticTerm() {
		Class<AndEntailment> aClass = AndEntailment.class;
		boolean thrown = false;
		try {
			aClass.getConstructor(new Class[] {
					Semantic.class, Term.class });
		} catch (Exception e) {
			thrown = true;
		}
		assertFalse(
				"Missing constructor with Semantic and Term parameters in AndEntailment class.",
				thrown);

		assertEquals(
				"AndEntailment class should extend RuleNode",
				RuleNode.class,
				AndEntailment.class.getSuperclass());

		AndEntailment e = and;
		Field f;
		try {
			f = e.getClass().getDeclaredField("contextRuisSet");

			f.setAccessible(true);
			f.set(e, new ContextRuisSet());

			assertNotNull(
					"The constructor of AndEntailment class should initialize inherited variables correctly by calling super.",
					f);
		} catch(Exception x){
			assertNull(x.getMessage(), x);
		}
	}

	@Test
	public void testAddNotSentRui() {
		and.addNotSentRui(rui, "default", dog);
		Context contxt = (Context) Controller.getContextByName("default");
		assertNotNull("AndEntailment: addNotSentRui doesn't add a RuiHandler in contextRuisHandlers", 
				and.getContextRuiHandler(contxt));

		NodeSet positives = and.getContextRuiHandler(contxt).getPositiveNodes();
		assertTrue("AndEntailment: addNotSentRui doesn't add signature to positiveNodes set", 
				positives.contains(dog));
		assertTrue("AndEntailment: addNotSentRui adds wrong signatures to positiveNodes set", 
				!positives.contains(fido));

		assertTrue("AndEntailment: addNotSentRui doesn't add a PTree in contextRuisHandlers", 
				and.getContextRuiHandler(contxt)instanceof PTree);

	}

	@AfterClass
	public void tearDown() throws Exception {
		Network.clearNetwork();
		and.clear();
		fido = null;
		var = null;
		dog = null;
		rui = null;
		report = null;
	}
}
