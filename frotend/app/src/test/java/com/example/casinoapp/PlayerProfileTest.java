package com.example.casinoapp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;

import android.content.Intent;
import android.view.View;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowActivity;
import org.robolectric.shadows.ShadowAlertDialog;
import org.robolectric.shadows.ShadowPopupMenu;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = {33})
public class PlayerProfileTest {

    @Test
    public void testActivityLaunchAndInitialization() {
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), PlayerProfile.class);
        intent.putExtra("playerName", "TestPlayer");

        try (ActivityScenario<PlayerProfile> scenario = ActivityScenario.launch(intent)) {
            scenario.onActivity(activity -> {
                // Verify Welcome Text
                TextView tvWelcome = activity.findViewById(R.id.tvWelcome);
                assertEquals("Γεια σου, TestPlayer!", tvWelcome.getText().toString());

                // Verify Login Name Text
                TextView tvLoginName = activity.findViewById(R.id.tvLoginName);
                assertEquals("Είσοδος Παίκτη: TestPlayer", tvLoginName.getText().toString());

                // Verify Header Username
                TextView tvHeaderUsername = activity.findViewById(R.id.tvHeaderUsername);
                assertEquals("TestPlayer", tvHeaderUsername.getText().toString());
            });
        }
    }

    @Test
    public void testNavigateToSearchGames() {
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), PlayerProfile.class);
        intent.putExtra("playerName", "TestPlayer");

        try (ActivityScenario<PlayerProfile> scenario = ActivityScenario.launch(intent)) {
            scenario.onActivity(activity -> {
                activity.findViewById(R.id.btnSearchGames).performClick();

                ShadowActivity shadowActivity = shadowOf(activity);
                Intent startedIntent = shadowActivity.getNextStartedActivity();

                assertNotNull(startedIntent);
                assertEquals(SearchAndPlayActivity.class.getName(), startedIntent.getComponent().getClassName());
                assertEquals("TestPlayer", startedIntent.getStringExtra("playerName"));
            });
        }
    }

    @Test
    public void testLogoutNavigation() {
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), PlayerProfile.class);
        intent.putExtra("playerName", "TestPlayer");

        try (ActivityScenario<PlayerProfile> scenario = ActivityScenario.launch(intent)) {
            scenario.onActivity(activity -> {
                activity.findViewById(R.id.btnLogout).performClick();

                ShadowActivity shadowActivity = shadowOf(activity);
                Intent startedIntent = shadowActivity.getNextStartedActivity();

                assertNotNull(startedIntent);
                assertEquals(RoleActivity.class.getName(), startedIntent.getComponent().getClassName());
                assertTrue(activity.isFinishing());
            });
        }
    }

    @Test
    public void testBackNavigation() {
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), PlayerProfile.class);
        intent.putExtra("playerName", "TestPlayer");

        try (ActivityScenario<PlayerProfile> scenario = ActivityScenario.launch(intent)) {
            scenario.onActivity(activity -> {
                activity.findViewById(R.id.btnBack).performClick();
                assertTrue(activity.isFinishing());
            });
        }
    }

    @Test
    public void testUserMenuProfile() {
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), PlayerProfile.class);
        intent.putExtra("playerName", "TestPlayer");

        try (ActivityScenario<PlayerProfile> scenario = ActivityScenario.launch(intent)) {
            scenario.onActivity(activity -> {
                View menuTrigger = activity.findViewById(R.id.imgMenuTrigger);
                menuTrigger.performClick();

                PopupMenu latestPopupMenu = ShadowPopupMenu.getLatestPopupMenu();
                assertNotNull(latestPopupMenu);

                // Simulate clicking Profile from the popup menu
                latestPopupMenu.getMenu().performIdentifierAction(R.id.menu_profile, 0);

                AlertDialog dialog = (AlertDialog) ShadowAlertDialog.getLatestDialog();
                assertNotNull(dialog);
                assertTrue(dialog.isShowing());
                
                TextView tvProfileName = dialog.findViewById(R.id.tvProfileName);
                if (tvProfileName != null) {
                    assertEquals("TestPlayer", tvProfileName.getText().toString());
                }
            });
        }
    }

    @Test
    public void testUserMenuSettings() {
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), PlayerProfile.class);
        intent.putExtra("playerName", "TestPlayer");

        try (ActivityScenario<PlayerProfile> scenario = ActivityScenario.launch(intent)) {
            scenario.onActivity(activity -> {
                View menuTrigger = activity.findViewById(R.id.imgMenuTrigger);
                menuTrigger.performClick();

                PopupMenu latestPopupMenu = ShadowPopupMenu.getLatestPopupMenu();
                assertNotNull(latestPopupMenu);

                latestPopupMenu.getMenu().performIdentifierAction(R.id.menu_settings, 0);
                
                ShadowActivity shadowActivity = shadowOf(activity);
                Intent startedIntent = shadowActivity.getNextStartedActivity();

                assertNotNull(startedIntent);
                assertEquals(SettingsActivity.class.getName(), startedIntent.getComponent().getClassName());
            });
        }
    }

    @Test
    public void testAddBalanceDialog() {
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), PlayerProfile.class);
        intent.putExtra("playerName", "TestPlayer");

        try (ActivityScenario<PlayerProfile> scenario = ActivityScenario.launch(intent)) {
            scenario.onActivity(activity -> {
                activity.findViewById(R.id.btnAddBalance).performClick();

                AlertDialog dialog = (AlertDialog) ShadowAlertDialog.getLatestDialog();
                assertNotNull(dialog);
                
                ShadowAlertDialog shadowDialog = (ShadowAlertDialog) shadowOf(dialog);
                assertEquals("Κατάθεση Tokens", shadowDialog.getTitle().toString());

                assertTrue(dialog.isShowing());
            });
        }
    }
}
