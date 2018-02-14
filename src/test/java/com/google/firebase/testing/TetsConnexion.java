package com.google.firebase.testing;

import java.io.FileInputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import org.junit.Test;

import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseCredentials;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class TetsConnexion {

	@Test
	public void testKeepSynced() throws Exception {

		FileInputStream serviceAccount = new FileInputStream(
				"D:/etech_project/firebase-admin-java-master/lifinstore-8760b-firebase-adminsdk-dyx8e-ecd83a0bce.json");

		FirebaseOptions options = new FirebaseOptions.Builder()
				.setCredential(FirebaseCredentials.fromCertificate(serviceAccount))
				.setDatabaseUrl("https://lifinstore-8760b.firebaseio.com").build();

		FirebaseApp.initializeApp(options);

		// As an admin, the app has access to read and write all data,
		// regardless of Security Rules
		DatabaseReference ref = FirebaseDatabase.getInstance().getReference("/data/lampes");

		DatabaseReference connectedRef = FirebaseDatabase.getInstance().getReference(".info/connected");
		connectedRef.addValueEventListener(new ValueEventListener() {
			public void onDataChange(DataSnapshot snapshot) {
				boolean connected = snapshot.getValue(Boolean.class);
				if (connected) {
					System.out.println("connected -------------------------------------------------");
				} else {
					System.out.println("not connected ------------------------------------------------------");
				}
			}

			public void onCancelled(DatabaseError error) {
				System.err.println("Listener was cancelled");
			}
		});

		DatabaseReference data = FirebaseDatabase.getInstance().getReference();

		try {
			DatabaseReference lampesJson = data.child("/data").child("/lampes");
			DatabaseReference refDB = lampesJson.child("6");
			final CountDownLatch latch = new CountDownLatch(1);
			refDB.child("latitude").setValue("49.74240353167144", new DatabaseReference.CompletionListener() {
				@Override
				public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
					if (databaseError != null) {
						System.out.println("Data could not be saved " + databaseError.getMessage());
						latch.countDown();
					} else {
						System.out.println("Data saved successfully.");
						latch.countDown();
					}
				}
			});
			refDB.child("longitude").setValue("4.599507965993882", new DatabaseReference.CompletionListener() {
				@Override
				public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
					if (databaseError != null) {
						System.out.println("Data could not be saved " + databaseError.getMessage());
						latch.countDown();
					} else {
						System.out.println("Data saved successfully.");
						latch.countDown();
					}
				}
			});
			latch.await();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		// ref.child("3").setValue(
		// "{\"latitude\" : 49.74240353167144,\"longitude\" :
		// 4.599507965993882,\"macAddress\" :
		// \"MAC_ADDRESS_20171218_16h11\",\"nom\" : \"Lampe
		// 20171218_16h11\",\"uuid\" :
		// \"3144a8e0-e3f7-11e7-9dc7-533811b21bfb\",\"uuidMagasins\" : [
		// \"3f811c30-e3f8-11e7-9dc7-533811b21bfb\" ],\"zIndex\" : 0}");
		//
		// final FirebaseDatabase database = FirebaseDatabase.getInstance();
		// // https://lifinstore-8760b.firebaseio.com/data/lampes/0/latitude
		// DatabaseReference adminRef = database.getReference("/data");
		//
		// DatabaseReference lampesRef = database.getReference("/data/lampes");
		//
		// DatabaseReference LAMPERef = adminRef.child("/lampes/");
		//
		// DatabaseReference hopperRef = LAMPERef.child("/0");
		//
		// Map<String, Object> hopperUpdates = new HashMap<String, Object>();
		// hopperUpdates.put("latitude", "300");
		//
		// hopperRef.updateChildrenAsync(hopperUpdates);

	}

}
