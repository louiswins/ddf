/**
 * Copyright (c) Codice Foundation
 *
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 *
 **/
package ddf.catalog.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.LoggerFactory;
import org.slf4j.ext.XLogger;

import ddf.catalog.source.CatalogProvider;
import ddf.catalog.source.ConnectedSource;
import ddf.catalog.source.FederatedSource;
import ddf.catalog.source.Source;
import ddf.catalog.source.SourceMonitor;

/**
 * The poller to check the availability of all configured sources. This class is instantiated
 * by the CatalogFramework's blueprint and is scheduled by the {@link SourcePoller} to execute
 * at a fixed rate, i.e., the polling interval. 
 * 
 * This class maintains a list of all of the sources
 * to be polled for their availability. Sources are added to this list when they come online and
 * when they are deleted. A cached map is maintained of all the sources and their last availability
 * states.
 * 
 * @author ddf.isgs@lmco.com
 *
 */
public class SourcePollerRunner implements Runnable {

	private List<Source> sources;
	private Map<Source, SourceStatus> status = new ConcurrentHashMap<Source, SourceStatus>();
	private static XLogger logger = new XLogger(LoggerFactory.getLogger(SourcePollerRunner.class));

	
	/**
	 * Creates an empty list of {@link Source} sources to be polled for availability.
	 * This constructor is invoked by the CatalogFramework's blueprint.
	 */
	public SourcePollerRunner() {

		logger.info("Creating source poller runner.");
		sources = new ArrayList<Source>();

	}

	
	/**
	 * Checks the availability of each source in the list of sources
	 * to be polled.
	 */
	@Override
	public void run() {

		logger.trace("RUNNER checking source statuses");

		for (Source source : sources) {

			if (source != null) {

				checkStatus(source);

			}

		}

	}

	
	/**
	 * Checks if the specified source is available, updating the internally maintained
	 * map of sources and their status.
	 * 
	 * @param source the source to check if it is available
	 */
	private void checkStatus(final Source source) {

		boolean available = false ;
		
		try {
			
			SourceMonitor monitor = new SourceMonitor(){

				@Override
				public void setAvailable() {
					status.put(source, SourceStatus.AVAILABLE);
					logger.info("Source [" + source+ "] with id ["+ source.getId() + "] is AVAILABLE");
				}

				@Override
				public void setUnavailable() {
					status.put(source, SourceStatus.UNAVAILABLE);
					logger.info("Source [" + source+ "] with id ["+ source.getId() + "] is UNAVAILABLE");
				}
			};
				
			available = source.isAvailable(monitor);
			
		} catch (Exception e) {
			logger.debug("Source [" + source + "] did not return properly with availability.",e) ;
		} 

		logger.trace("Source [" + source+ "] with id ["
				+ source.getId() + "] isAvailable? " + available);

		if (available) {
			status.put(source, SourceStatus.AVAILABLE);
		} else {
			status.put(source, SourceStatus.UNAVAILABLE);
		}

	}
	
	
	/**
	 * Adds the {@link Source} instance to the list and sets its current status
	 * to UNCHECKED, indicating it will checked at the next polling interval.
	 * 
	 * @param source the source to add to the list
	 */
	public void bind(Source source) {
		
		logger.info("Binding source: " + source);
		if (source != null) {
			logger.debug("Marking new source " + source+ " as UNCHECKED.") ;
			sources.add(source);
			status.put(source, SourceStatus.UNCHECKED);
		}
	}
	
	
	/**
	 * Removes the {@link Source} instance from the list of references
	 * so that its availability is no longer polled.
	 * 
	 * @param source the source to remove from the list
	 */
	public void unbind(Source source) {
		logger.info("Unbinding source [" + source +"]");
		if(source != null) {
			status.remove(source) ;
			sources.remove(source);
		}
	}

	
	/**
	 * Retrieve the current availability of the specified source.
	 * 
	 * @param source the source to retrieve the availability for
	 * 
	 * @return the source's status
	 */
	public SourceStatus getStatus(Source source) {
		return status.get(source);
	}
	
}
