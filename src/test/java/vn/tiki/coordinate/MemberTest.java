package vn.tiki.coordinate;

import io.gridgo.bean.BObject;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class MemberTest {

	MemberImpl member1;

	@Before
	public void setUp() {
		member1 = MemberImpl.builder().id("member1").data(BObject.ofEmpty()).build();
	}

	@Test
	public void test_toBytes_and_toString() {
		byte[] bytes = member1.toBytes();
		assertTrue(bytes.length > 8);
		String s = member1.toString();
		assertTrue(s.startsWith("member1 -> "));
	}

	@Test
	public void test_Equals() {
		MemberImpl member2 = MemberImpl.builder().id("member1").data(BObject.ofEmpty()).build();
		MemberImpl member3 = MemberImpl.builder().id("member3").data(BObject.ofEmpty()).build();
		assertTrue(member1.equals(member2));
		assertFalse(member1.equals(member3));
		assertFalse(member2.equals("not_the_object"));
	}

	@Test
	public void test_toBytes() {
		byte[] bytes = member1.toBytes();
		Member member = Member.fromBytes(bytes);
		assertTrue(member.equals(member1));
	}

	@Test
	public void test_newBuilder() {
		MemberImpl.MemberBuilder memberBuilder = Member.newBuilder();
		assertNotNull(memberBuilder);
	}
}