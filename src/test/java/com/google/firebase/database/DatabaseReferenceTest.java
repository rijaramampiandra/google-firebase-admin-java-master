/*
 * Copyright 2017 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.firebase.database;

import static com.google.firebase.database.TestHelpers.mockRepo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.times;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Map;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.TestOnlyImplFirebaseTrampolines;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference.CompletionListener;
import com.google.firebase.database.Transaction.Handler;
import com.google.firebase.database.Transaction.Result;
import com.google.firebase.database.core.CompoundWrite;
import com.google.firebase.database.core.DatabaseConfig;
import com.google.firebase.database.core.Path;
import com.google.firebase.database.core.Repo;
import com.google.firebase.database.core.ServerValues;
import com.google.firebase.database.snapshot.ChildKey;
import com.google.firebase.database.snapshot.Node;
import com.google.firebase.database.snapshot.NodeUtilities;
import com.google.firebase.testing.ServiceAccount;

@Ignore
public class DatabaseReferenceTest {

	private static final String DB_URL = "https://lifinstore-8760b.firebaseio.com";
	private static final Path path = new Path("foo");

	private static FirebaseApp testApp;
	private static DatabaseConfig config;

	@BeforeClass
	public static void setUpClass() throws IOException {
		testApp = FirebaseApp.initializeApp(new FirebaseOptions.Builder()
				.setCredentials(GoogleCredentials.fromStream(ServiceAccount.EDITOR.asStream())).setDatabaseUrl(DB_URL)
				.setProjectId("lifinstore-8760b").setStorageBucket("lifinstore-8760b.appspot.com").build());
		// Obtain a new DatabaseConfig instance for testing. Since we are not
		// connecting to an
		// actual Firebase database, it is necessary to use a stand-in
		// DatabaseConfig here.
		config = TestHelpers.newTestConfig(testApp);
	}

	@AfterClass
	public static void tearDownClass() throws InterruptedException {
		// Tear down and clean up the test DatabaseConfig.
		TestHelpers.interruptConfig(config);
		TestOnlyImplFirebaseTrampolines.clearInstancesForTest();
	}

	@Test
	public void testRootReference() throws IOException {
		FileInputStream serviceAccount = new FileInputStream("D:/google-services.json");

		// FirebaseOptions defaultOptions = new FirebaseOptions.Builder()
		// .setCredentials(GoogleCredentials.fromStream(serviceAccount))
		// .setDatabaseUrl("https://lifinstore-8760b.firebaseio.com")
		// .build();
		//
		// FirebaseApp firebase = FirebaseApp.initializeApp(defaultOptions);

		FirebaseOptions defaultOptions = new FirebaseOptions.Builder()
				.setCredentials(GoogleCredentials.getApplicationDefault()).setProjectId("lifinstore-8760b")
				.setStorageBucket("lifinstore-8760b.appspot.com").setDatabaseUrl("lifinstore-8760b.firebaseio.com/")
				.build();

		// Initialize the default app
		FirebaseApp defaultApp = FirebaseApp.initializeApp(defaultOptions);

		System.out.println(defaultApp.getName()); // "[DEFAULT]"

		// Retrieve services by passing the defaultApp variable...
		// FirebaseAuth defaultAuth = FirebaseAuth.getInstance(defaultApp);
		// FirebaseDatabase defaultDatabase =
		// FirebaseDatabase.getInstance(defaultApp);

		// ... or use the equivalent shorthand notation
		FirebaseAuth defaultAuth = FirebaseAuth.getInstance();
		FirebaseDatabase defaultDatabase = FirebaseDatabase.getInstance();
		DatabaseReference dbRef = defaultDatabase.getReference();
		DatabaseReference data = dbRef.child("data");

		DatabaseReference reference = new DatabaseReference(DB_URL, config);
		DatabaseReference ref = FirebaseDatabase.getInstance()
				.getReferenceFromUrl("https://lifinstore-8760b.firebaseio.com/lampes");
		DatabaseReference refs = FirebaseDatabase.getInstance().getReference().child("lampes").push();

		reference.child("data").child("lampes").setValue("user");

		assertNull(reference.getKey());
		assertNull(reference.getParent());
		assertEquals(ImmutableList.of(), reference.getPath().asList());
		assertEquals(DB_URL, reference.toString());
		assertNotNull(reference.getDatabase());
	}

	@Test
	public void testChild() {
		DatabaseReference reference = new DatabaseReference(DB_URL, config);
		DatabaseReference child = reference.child("bar");
		DatabaseReference lampes = reference.child("lampes");
		assertNotNull(child);
		assertNotNull(lampes);
		assertEquals("bar", child.getKey());
		assertEquals(reference, child.getParent());
		assertEquals(ImmutableList.of("bar"), child.getPath().asList());
		assertEquals(DB_URL + "/bar", child.toString());
		assertNotEquals(reference.hashCode(), child.hashCode());
		assertSame(reference.getDatabase(), child.getDatabase());

		try {
			reference.child(null);
			fail("No error thrown for null child key");
		} catch (NullPointerException expected) {
			// expected
		}
	}

	@Test
	public void testChildWithSpace() {
		DatabaseReference reference = new DatabaseReference(DB_URL, config);
		DatabaseReference child = reference.child("bar 1");
		assertNotNull(child);
		assertEquals("bar 1", child.getKey());
		assertEquals(reference, child.getParent());
		assertEquals(ImmutableList.of("bar 1"), child.getPath().asList());
		assertEquals(DB_URL + "/bar%201", child.toString());
	}

	@Test
	public void testPush() {
		DatabaseReference reference = new DatabaseReference(DB_URL, config);
		DatabaseReference child = reference.push();
		assertNotNull(child);
		assertTrue(!Strings.isNullOrEmpty(child.getKey()));
		assertEquals(reference, child.getParent());
		assertEquals(ImmutableList.of(child.getKey()), child.getPath().asList());
	}

	@Test
	public void testSetValue() throws Exception {
		Repo repo = mockRepo();
		DatabaseReference reference = new DatabaseReference(repo, path);
		reference.setValueAsync("value");
		reference.setValue("value", null);
		Mockito.verify(repo, times(2)).scheduleNow(Mockito.any(Runnable.class));
		Mockito.verify(repo, times(2)).setValue(Mockito.same(path), Mockito.eq(NodeUtilities.NodeFromJSON("value")),
				Mockito.any(CompletionListener.class));
	}

	@Test
	public void testSetValueWithPriority() throws Exception {
		Repo repo = mockRepo();
		DatabaseReference reference = new DatabaseReference(repo, path);
		reference.setValueAsync("value", 10);
		reference.setValue("value", 10, null);
		Mockito.verify(repo, times(2)).scheduleNow(Mockito.any(Runnable.class));
		Mockito.verify(repo, times(2)).setValue(Mockito.same(path),
				Mockito.eq(NodeUtilities.NodeFromJSON(ImmutableMap.of(".value", "value", ".priority", 10))),
				Mockito.any(CompletionListener.class));
	}

	@Test
	public void testSetPriority() throws Exception {
		Repo repo = mockRepo();
		DatabaseReference reference = new DatabaseReference(repo, path);
		reference.setPriorityAsync(10);
		reference.setPriority(10, null);
		Mockito.verify(repo, times(2)).scheduleNow(Mockito.any(Runnable.class));
		Mockito.verify(repo, times(2)).setValue(Mockito.eq(path.child(ChildKey.fromString(".priority"))),
				Mockito.eq(NodeUtilities.NodeFromJSON(10.0)), Mockito.any(CompletionListener.class));
	}

	@Test
	public void testUpdateChildren() throws Exception {
		Map<String, Object> update = ImmutableMap.<String, Object>of("foo", "bar");

		Repo repo = mockRepo();
		DatabaseReference reference = new DatabaseReference(repo, path);
		reference.updateChildrenAsync(update);
		reference.updateChildren(update, null);
		Mockito.verify(repo, times(2)).scheduleNow(Mockito.any(Runnable.class));
		Mockito.verify(repo, times(2)).updateChildren(Mockito.same(path), Mockito.any(CompoundWrite.class),
				Mockito.any(CompletionListener.class), Mockito.eq(update));
	}

	@Test
	public void testRemoveValue() throws Exception {
		Repo repo = mockRepo();
		DatabaseReference reference = new DatabaseReference(repo, path);
		reference.removeValueAsync();
		reference.removeValue(null);
		Mockito.verify(repo, times(2)).scheduleNow(Mockito.any(Runnable.class));
		Mockito.verify(repo, times(2)).setValue(Mockito.same(path), Mockito.eq(NodeUtilities.NodeFromJSON(null)),
				Mockito.any(CompletionListener.class));
	}

	@Test
	public void testRunTransaction() throws Exception {
		Repo repo = mockRepo();
		DatabaseReference reference = new DatabaseReference(repo, path);
		try {
			reference.runTransaction(null);
			fail("No error thrown for null handler");
		} catch (NullPointerException expected) {
			// expected
		}

		try {
			reference.runTransaction(null, true);
			fail("No error thrown for null handler");
		} catch (NullPointerException expected) {
			// expected
		}

		Handler handler = new Handler() {
			@Override
			public Result doTransaction(MutableData currentData) {
				return null;
			}

			@Override
			public void onComplete(DatabaseError error, boolean committed, DataSnapshot currentData) {

			}
		};

		reference.runTransaction(handler);
		reference.runTransaction(handler, true);
		Mockito.verify(repo, times(2)).scheduleNow(Mockito.any(Runnable.class));
		Mockito.verify(repo, times(2)).startTransaction(Mockito.same(path), Mockito.same(handler),
				Mockito.anyBoolean());
	}

	@Test
	public void testOnDisconnect() {
		DatabaseReference reference = new DatabaseReference(DB_URL, config);
		assertNotNull(reference.onDisconnect());
	}

	@Test
	public void testSetHijackHash() throws Exception {
		Repo repo = mockRepo();
		DatabaseReference reference = new DatabaseReference(repo, path);
		reference.setHijackHash(true);
		Mockito.verify(repo, times(1)).scheduleNow(Mockito.any(Runnable.class));
		Mockito.verify(repo, times(1)).setHijackHash(true);
	}

	@Test
	public void testServerValue() {
		assertNotNull(ServerValue.TIMESTAMP);
		assertTrue(ServerValue.TIMESTAMP.containsKey(ServerValues.NAME_SUBKEY_SERVERVALUE));
	}

	@Test
	public void testTransactionResult() {
		Result result = Transaction.abort();
		assertFalse(result.isSuccess());
		assertNull(result.getNode());

		Node node = NodeUtilities.NodeFromJSON("node");
		result = Transaction.success(new MutableData(node));
		assertTrue(result.isSuccess());
		assertSame(node, result.getNode());
	}
}
