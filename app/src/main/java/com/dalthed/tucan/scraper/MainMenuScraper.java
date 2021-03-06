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

package com.dalthed.tucan.scraper;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.util.Log;
import android.widget.ListAdapter;

import com.dalthed.tucan.R;
import com.dalthed.tucan.TucanMobile;
import com.dalthed.tucan.Connection.AnswerObject;
import com.dalthed.tucan.Connection.CookieManager;
import com.dalthed.tucan.adapters.OneLineTwoColsAdapter;
import com.dalthed.tucan.exceptions.LostSessionException;
import com.dalthed.tucan.exceptions.TucanDownException;

public class MainMenuScraper extends BasicScraper {

	/**
	 * Set true if User has selected English
	 */
	public static boolean isEnglish = false;
	public String SessionArgument;
	public boolean noeventstoday = false;
	/**
	 * Array an Links zu den Seiten der heutigen Veranstaltungen
	 */
	public String[] today_event_links;
	/**
	 * Der Name des Users
	 */
	public String UserName;
	/**
	 * URL zum Vorlesungsverzeichnis
	 */
	public String menu_link_vv;
	/**
	 * URL zu den Prüfungen
	 */
	public String menu_link_ex;
	/**
	 * URL zu den Nachrichten
	 */
	public String menu_link_msg;
	/**
	 * Stores link of Event location overview
	 */
	public String load_link_ev_loc;
	/**
	 * Last called URl
	 */
	public URL lcURL;
	/**
	 * URL zu Stundenplan
	 */
	public String menu_link_month;
	/**
	 * Gibt an, ob die Tucan-Webseite eine Bewerbungsseite darstellt.
	 */
	public boolean isApplication = false;

	public MainMenuScraper(Context context, AnswerObject result) {
		super(context, result);
	}

	/**
	 * Getter and Setter
	 */
	public boolean getIsEnglish() {
		return isEnglish;
	}

	public void setIsEnglish(boolean isEnglish) {
		MainMenuScraper.isEnglish = isEnglish;
	}

	@Override
	public ListAdapter scrapeAdapter(int mode) throws LostSessionException, TucanDownException {
		
		if (checkForLostSeesion()) {
			if(TucanMobile.CRASH) {
				//Crash with HTML
				ArrayList<String> crashList = new ArrayList<String>();
				crashList.add("fail");
				crashList.get(16);
			}
			getSessionArgument();
			scrapeMenuLinks();
			return getTodaysEvents();
		}
		return null;
	}

	/**
	 * Checks if Tucan is set to german. Otherwise, the App wont work
	 * 
	 * @param context
	 *            Activity context
	 * @since 2012-07-17
	 * @author Daniel Thiem
	 */
	public void checkForRightTucanLanguage(final Activity context) {

//		if (doc.select("li#link000326").select("a").attr("href").equals("")) {
//			Dialog wronglanguageDialog = new AlertDialog.Builder(context).setTitle("")
//					.setMessage(R.string.general_not_supported_lang)
//					.setPositiveButton("Okay", new DialogInterface.OnClickListener() {
//
//						public void onClick(DialogInterface dialog, int which) {
//							context.finish();
//						}
//					}).create();
//			wronglanguageDialog.show();
//
//		}
		if (doc.select("li#link000326").select("a").attr("href").equals("")) {
			setIsEnglish(true);
			scrapeMenuLinks();
		}
	}

	public void bufferLinks(Activity context, CookieManager localCookieManager) {
		try {
			FileOutputStream fos = context.openFileOutput(TucanMobile.LINK_FILE_NAME,
					Context.MODE_PRIVATE);

			StringBuilder cacheString = new StringBuilder();
			cacheString.append(menu_link_vv).append(">>").append(menu_link_month).append(">>")
					.append(menu_link_ex).append(">>").append(menu_link_ex).append(">>")
					.append(menu_link_msg).append("<<")
					.append(localCookieManager.getCookieHTTPString(TucanMobile.TUCAN_HOST))
					.append("<<").append(SessionArgument);
			fos.write(cacheString.toString().getBytes());
			fos.close();

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * @return
	 */
	private ListAdapter getTodaysEvents() {
		// Tabelle mit den Terminen finden und Durchlaufen
		Element EventTable = doc.select("table.nb").first();

		String[] Events;
		String[] Times;

		if (EventTable == null) {
			// Falls keine Events gefunden werden, wird das angezeigt
			Events = new String[1];
			Times = new String[1];
//			Events[0] = "Keine Heutigen Veranstaltungen";
			Events[0] = context.getString(R.string.no_events_today);
			Times[0] = "";
			noeventstoday = true;
		} else {

			if (EventTable.select("tr.tbdata").first().select("td").size() == 5) {
				// Wen die Anzahl der Spalten der entsprechenden Tabelle 5
				// ist, ist das ein Anzeichen dafür, dass heute keine
				// Veranstaltungen sind
				Events = new String[1];
				Times = new String[1];
//				Events[0] = "Keine Heutigen Veranstaltungen";
				Events[0] = context.getString(R.string.no_events_today);
				Times[0] = "";
				noeventstoday = true;
			} else {

				// Nehme die einzelnen Event-Zeilen heraus und gehe diese durch
				Elements EventRows = EventTable.select("tr.tbdata");
				Iterator<Element> RowIt = EventRows.iterator();
				Events = new String[EventRows.size()];
				Times = new String[EventRows.size()];
				today_event_links = new String[EventRows.size()];
				int i = 0;
				while (RowIt.hasNext()) {
					Element currentElement = (Element) RowIt.next();
					// Informationen aus HTML parsen
					String EventString = currentElement.select("td[headers=Name]").select("a")
							.first().text();
					today_event_links[i] = currentElement.select("td[headers=Name]").select("a")
							.first().attr("href");

					// Zeit zusammenfügen
					String EventTimeString = currentElement.select("td").get(2).select("a").first()
							.text();
					String EventTimeEndString = currentElement.select("td").get(3).select("a")
							.first().text();
					Times[i] = EventTimeString + "-" + EventTimeEndString;

					Events[i] = EventString;
					i++;
				}

			}
		}

		ListAdapter returnAdapter = new OneLineTwoColsAdapter(context, Events, Times);

		return returnAdapter;
	}

	/**
	 * 
	 */
	private void scrapeMenuLinks() {
		UserName = doc.select("span#loginDataName").text().split(":")[1];

		lcURL = null;
		try {
			lcURL = new URL(lastCalledUrl);
		} catch (MalformedURLException e) {
			Log.e(LOG_TAG, "Malformed URL");
		}
		Elements linkstoOuterWorld = doc.select("div.tb");
		if(linkstoOuterWorld.size() > 1) {
//			Element ArchivLink = linkstoOuterWorld.get(1).select("a").first();
//			menu_link_month = lcURL.getProtocol() + "://" + lcURL.getHost()
//					+ doc.select("li#link000271").select("a").attr("href");
//			menu_link_vv = lcURL.getProtocol() + "://" + lcURL.getHost()
//					+ doc.select("li#link000326").select("a").attr("href");
//			menu_link_ex = lcURL.getProtocol() + "://" + lcURL.getHost()
//					+ doc.select("li#link000280").select("a").attr("href");
//			menu_link_msg = lcURL.getProtocol() + "://" + lcURL.getHost() + ArchivLink.attr("href");
//			// Load special Location Information
//			load_link_ev_loc = TucanMobile.TUCAN_PROT + TucanMobile.TUCAN_HOST
//					+ doc.select("li#link000269").select("a").attr("href");
			if (!getIsEnglish()) {
				Element ArchivLink = linkstoOuterWorld.get(1).select("a").first();
				menu_link_month = lcURL.getProtocol() + "://" + lcURL.getHost()
						+ doc.select("li#link000271").select("a").attr("href");
				menu_link_vv = lcURL.getProtocol() + "://" + lcURL.getHost()
						+ doc.select("li#link000326").select("a").attr("href");
				menu_link_ex = lcURL.getProtocol() + "://" + lcURL.getHost()
						+ doc.select("li#link000280").select("a").attr("href");
				menu_link_msg = lcURL.getProtocol() + "://" + lcURL.getHost() + ArchivLink.attr("href");
				// Load special Location Information
				load_link_ev_loc = TucanMobile.TUCAN_PROT + TucanMobile.TUCAN_HOST
						+ doc.select("li#link000269").select("a").attr("href");
			} else {
				Element ArchivLink = linkstoOuterWorld.get(1).select("a").first();
				menu_link_month = lcURL.getProtocol() + "://" + lcURL.getHost()
						+ doc.select("li#link000057").select("a").attr("href");
				menu_link_vv = lcURL.getProtocol() + "://" + lcURL.getHost()
						+ doc.select("li#link000352").select("a").attr("href");
				menu_link_ex = lcURL.getProtocol() + "://" + lcURL.getHost()
						+ doc.select("li#link000360").select("a").attr("href");
				menu_link_msg = lcURL.getProtocol() + "://" + lcURL.getHost() + ArchivLink.attr("href");
				// Load special Location Information
				load_link_ev_loc = TucanMobile.TUCAN_PROT + TucanMobile.TUCAN_HOST
						+ doc.select("li#link000055").select("a").attr("href");
			}
		} else {
			//Bewerbungsmodus
			isApplication = true;
		}
	}

	/**
	 * 
	 */
	private void getSessionArgument() {
		// Die Session ID aus URL gewinnen
		if (!TucanMobile.TESTING) {
			try {
				lcURL = new URL(lastCalledUrl);
				SessionArgument = lcURL.getQuery().split("ARGUMENTS=")[1].split(",")[0];
			} catch (MalformedURLException e) {

				e.printStackTrace();
			}
		}
	}
}
