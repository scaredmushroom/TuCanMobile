/**
 *	This file is part of TuCan Mobile.
 *
 *	TuCan Mobile is free software: you can redistribute it and/or modify
 *	it under the terms of the GNU General Public License as published by
 *	the Free Software Foundation, either version 3 of the License, or
 *	(at your option) any later version.
 *
 *	TuCan Mobile is distributed in the hope that it will be useful,
 *	but WITHOUT ANY WARRANTY; without even the implied warranty of
 *	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *	GNU General Public License for more details.
 *
 *	You should have received a copy of the GNU General Public License
 *	along with TuCan Mobile.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.dalthed.tucan.ui;

import java.net.MalformedURLException;
import java.net.URL;

import org.acra.ACRA;

import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.View;
import android.widget.ListAdapter;
import android.widget.ListView;

import com.dalthed.tucan.R;
import com.dalthed.tucan.TuCanMobileActivity;
import com.dalthed.tucan.TucanMobile;
import com.dalthed.tucan.Connection.AnswerObject;
import com.dalthed.tucan.Connection.CookieManager;
import com.dalthed.tucan.Connection.RequestObject;
import com.dalthed.tucan.Connection.SimpleSecureBrowser;
import com.dalthed.tucan.exceptions.LostSessionException;
import com.dalthed.tucan.exceptions.TucanDownException;
import com.dalthed.tucan.scraper.BasicScraper;
import com.dalthed.tucan.scraper.ScheduleScraper;
import com.dalthed.tucan.util.ConfigurationChangeStorage;

public class Schedule extends SimpleWebListActivity {

	ScheduleScraper scrape = null;
	private String URLStringtoCall;
	private ListAdapter adapter;

	@Override
	protected void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState, true, 1);
		setContentView(R.layout.schedule);

		String CookieHTTPString = getIntent().getExtras().getString("Cookie");
		URLStringtoCall = getIntent().getExtras().getString("URL");
		URL URLtoCall;
		if (!restoreResultBrowser()) {
			try {
				URLtoCall = new URL(URLStringtoCall);
				CookieManager localCookieManager = new CookieManager();
				localCookieManager.generateManagerfromHTTPString(URLtoCall.getHost(),
						CookieHTTPString);
				callResultBrowser = new SimpleSecureBrowser(this);
				RequestObject thisRequest = new RequestObject(URLStringtoCall, localCookieManager,
						RequestObject.METHOD_GET, "");

				callResultBrowser.execute(thisRequest);
			} catch (MalformedURLException e) {
				ACRA.getErrorReporter().handleSilentException(e);
			}
		}

	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		setContentView(R.layout.schedule);
	}

	public void onPostExecute(AnswerObject result) {
		if (adapter != null) {
			setListAdapter(adapter);
			return;
		}
		if (scrape == null) {
			scrape = new ScheduleScraper(this, result);
		} else {
			scrape.setNewAnswer(result);
		}
		try {
			adapter = scrape.scrapeAdapter(0);
			if (adapter != null) {
				setListAdapter(adapter);
			}
		} catch (LostSessionException e) {
			Intent BackToLoginIntent = new Intent(this, TuCanMobileActivity.class);
			BackToLoginIntent.putExtra("lostSession", true);
			startActivity(BackToLoginIntent);
		} catch (TucanDownException e) {
			TucanMobile.alertOnTucanDown(this, e.getMessage());
		}

	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		if (scrape != null) {
			Intent singleEventIntent = new Intent(Schedule.this, FragmentSingleEvent.class);
			singleEventIntent.putExtra("PREPLink", true);
			singleEventIntent.putExtra("URL", TucanMobile.TUCAN_PROT + TucanMobile.TUCAN_HOST
					+ scrape.appointments.get(position).link);
			singleEventIntent.putExtra("Cookie",
					scrape.getCookieManager().getCookieHTTPString(TucanMobile.TUCAN_HOST));
			startActivity(singleEventIntent);
		}
	}

	@Override
	public ConfigurationChangeStorage saveConfiguration() {
		ConfigurationChangeStorage cStore = new ConfigurationChangeStorage();
//		cStore.adapters.add(adapter);
		cStore.adapters.add(getListAdapter());
		cStore.addScraper(scrape);
		return cStore;
	}

	@Override
	public void retainConfiguration(ConfigurationChangeStorage conf) {
		BasicScraper retainedScraper = conf.getScraper(0, this);
		if (retainedScraper instanceof ScheduleScraper) {
			scrape = (ScheduleScraper) retainedScraper;
		}
		if (conf.adapters.get(0) != null) {
			setListAdapter(conf.adapters.get(0));
		}
	}

}
