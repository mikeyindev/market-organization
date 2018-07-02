/*
 * Copyright 1997-2013 Fabien Michel, Olivier Gutknecht, Jacques Ferber
 * 
 * This file is part of MaDKit_Demos.
 * 
 * MaDKit_Demos is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * MaDKit_Demos is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with MaDKit_Demos. If not, see <http://www.gnu.org/licenses/>.
 */
package madkit.marketorg;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Toolkit;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;

import madkit.gui.AgentFrame;
import madkit.gui.OutputPanel;
import madkit.kernel.Agent;
import madkit.kernel.Message;
import madkit.message.IntegerMessage;
import madkit.message.StringMessage;

/**
 * @author Fabien Michel, Olivier Gutknecht, Jacques Ferber
 * @version 5.1
 */
public class Provider extends Agent {

    /**
     * 
     */
    private static final long serialVersionUID = 9125493540160734521L;
    
    private static Map<String, Double> transportBaseCost = new HashMap<>();
    
    public static List<String> availableTransports =
            Arrays.asList("train", "boat", "plane", "bus");

    final private static Map<String, ImageIcon> icons = new HashMap<>();
    
    // Default 30% markup
    private double markup = 1.3;
    
    private double operatingCost;
    
    private int bidAmount;
    
    // Static initializer
    static {
		for (String competence : availableTransports) {
			icons.put(competence, new ImageIcon(Provider.class.getResource(
					"images/" + competence + ".png")));
		}
    
        // Base cost for each mode of transport
        transportBaseCost.put( "train", 100.0);
        transportBaseCost.put( "boat", 200.0 );
        transportBaseCost.put( "plane", 300.0 );
        transportBaseCost.put( "bus", 50.0 );
    }

    private static int nbOfProvidersOnScreen = 0;

    private String competence;
    private JPanel blinkPanel;

    public Provider() {
        competence = Provider.availableTransports.get(
				(int) (Math.random() * Provider.availableTransports.size()));
        
        // Calculate Operating Cost
		operatingCost = (Math.random() + 1.0) * transportBaseCost.get( competence );
    }

    public void activate() {
		createGroupIfAbsent(MarketOrganization.COMMUNITY,
                            MarketOrganization.PROVIDERS_GROUP,
                            true, null);
		requestRole(MarketOrganization.COMMUNITY,
                    MarketOrganization.PROVIDERS_GROUP,
                    competence + "-" + MarketOrganization.PROVIDER_ROLE,
                    null);
    }

    public void live() {
		while (true) {
			Message m = waitNextMessage();
			if (m.getSender().getRole().equals(MarketOrganization.BROKER_ROLE))
			    handleBrokerMessage((StringMessage) m);
			else
			    finalizeContract((StringMessage) m);
		}
    }
    
    /**
     * Calculate the amount to submit as bid.
     *
     * @return
     */
    private void calculateBid() {
        // The 1st term is the Provider's base cost, the 2nd term is base
        // cost for that mode of transport, the 3rd term is the markup
        bidAmount = (int)(operatingCost * markup);
    }

    private void handleBrokerMessage(StringMessage m) {
		if (m.getContent().equals("make-bid-please")) {
			getLogger().info("I received a call for bid from " + m.getSender());
//			sendReply(m, new IntegerMessage((int) (Math.random() * 500)));
            
            if (markup > 1.0) {
                markup -= 0.1; // Discount by 10%
            }
            
            this.calculateBid();
            
            // Broadcast bidAmount
            getLogger().info( "I am bidding " + bidAmount);
            sendReply( m, new IntegerMessage( bidAmount ));
            
//            if (markup > 1.0) {
//                markup -= 0.1; // Discount by 10%
//            }
//            getLogger().info( "I have lowered my markup to " + markup );
		}
		else {
            iHaveBeenSelected(m);
            markup += 0.2;
//            getLogger().info( "I have increased my markup to " + markup );
        }
    }
    
    
    // Won bid
    private void iHaveBeenSelected(StringMessage m) {
		if (hasGUI()) {
			blinkPanel.setBackground(Color.YELLOW);
		}
		getLogger().info("I have been selected :)");
		String contractGroup = m.getContent();
		createGroup(MarketOrganization.COMMUNITY, contractGroup, true);
		requestRole(MarketOrganization.COMMUNITY, contractGroup, MarketOrganization.PROVIDER_ROLE);
		sendReply(m, new Message()); // just an acknowledgment
    }

    private void finalizeContract(StringMessage m) {
		if (hasGUI()) {
			blinkPanel.setBackground(Color.GREEN);
		}
		
		getLogger().info("I have sold something: That's great !");
		sendReply(m, new StringMessage("ticket"));
		pause((int) (Math.random() * 2000 + 1000));// let us celebrate !!
		leaveGroup(MarketOrganization.COMMUNITY, m.getSender().getGroup());
		
		if (hasGUI()) {
			blinkPanel.setBackground(Color.LIGHT_GRAY);
		}
    }

    @Override
    public void setupFrame(AgentFrame frame) {
		JPanel p = new JPanel(new BorderLayout());
		// customizing but still using the madkit.gui.OutputPanel.OutputPanel
		p.add(new OutputPanel(this), BorderLayout.CENTER);
		blinkPanel = new JPanel();
		blinkPanel.add(new JLabel(icons.get(competence)));
		p.add(blinkPanel, BorderLayout.NORTH);
		blinkPanel.setBackground(Color.LIGHT_GRAY);
		p.validate();
		frame.add(p);
		int xLocation = nbOfProvidersOnScreen++ * 300;
		if (xLocation + 300 > Toolkit.getDefaultToolkit().getScreenSize().getWidth())
			nbOfProvidersOnScreen = 0;
		frame.setLocation(xLocation, 640);
		frame.setSize(300, 300);
    }
}
