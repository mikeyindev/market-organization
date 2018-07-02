/*
 * Copyright 1997-2012 Fabien Michel, Olivier Gutknecht, Jacques Ferber
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
import java.awt.Image;
import java.awt.Toolkit;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;

import madkit.action.AgentAction;
import madkit.gui.AgentFrame;
import madkit.gui.OutputPanel;
import madkit.kernel.Agent;
import madkit.kernel.Message;
import madkit.message.IntegerMessage;
import madkit.message.ObjectMessage;
import madkit.message.StringMessage;

/**
 * @author Fabien Michel, Olivier Gutknecht, Jacques Ferber
 * @version 5.1
 */
public class Broker extends Agent {

    /**
     * 
     */
    private static final long serialVersionUID = 1217908977100108396L;

    static int nbOfBrokersOnScreen = 0;

    private static ImageIcon brokerImage = new ImageIcon(new ImageIcon(Broker.class.getResource("images/broker.png")).getImage().getScaledInstance(70, 70, Image.SCALE_SMOOTH));
    private JPanel blinkPanel;

    @Override
    protected void activate() {
        createGroupIfAbsent(MarketOrganization.COMMUNITY, MarketOrganization.CLIENT_GROUP, true, null);
        createGroupIfAbsent(MarketOrganization.COMMUNITY, MarketOrganization.PROVIDERS_GROUP, true, null);
        requestRole(MarketOrganization.COMMUNITY, MarketOrganization.CLIENT_GROUP, MarketOrganization.BROKER_ROLE, null);
        requestRole(MarketOrganization.COMMUNITY, MarketOrganization.PROVIDERS_GROUP, MarketOrganization.BROKER_ROLE, null);
    }

    @Override
    protected void live() {
		while (isAlive()) {
			Message m = purgeMailbox();// to always treat the latest request
			if (m == null) {
				m = waitNextMessage();// waiting a request
			}
			String role = m.getSender().getRole();
			if (role.equals(MarketOrganization.CLIENT_ROLE)) {
				handleClientRequest((StringMessage) m);
			}
		}
    }

    private void handleClientRequest(StringMessage request) {
		if (!checkAgentAddress(request.getSender())) // Is the client still there ?
			return;
		if (hasGUI()) { // starting the contract net
			blinkPanel.setBackground(Color.YELLOW);
		}
		getLogger().info(() -> "I received a request for a " +
				request.getContent() + " \nfrom " + request.getSender());
		
		//get proposals from providers
		List<Message> bids = getBids(request);
		if(bids == null)
			return;
	
		IntegerMessage best = selectBestOffer(bids);
	
		makeTransactionHappenBetweenClientAndProvider(request, best);
		
		if (hasGUI()) {
			blinkPanel.setBackground(Color.LIGHT_GRAY);
		}
    }

    /**
     * @param request
     * @return
     */
    private List<Message> getBids(StringMessage request) {
		List<Message> bids = broadcastMessageWithRoleAndWaitForReplies(// wait all answers
			MarketOrganization.COMMUNITY, // target community
			MarketOrganization.PROVIDERS_GROUP, // target group
			request.getContent() + "-" + MarketOrganization.PROVIDER_ROLE, // target role
			new StringMessage("make-bid-please"), // ask for a bid
			MarketOrganization.BROKER_ROLE, // I am a broker: Let the receiver know about it
			900); // I cannot wait the end of the universe

		if (bids == null) { // no reply
			getLogger().info(() -> "No bids at all : No one is selling " +
					request.getContent().toUpperCase() + " !!\nPlease launch other providers !");
			if (hasGUI()) {
				blinkPanel.setBackground(Color.LIGHT_GRAY);
			}
			return null;
		}
		return bids;
    }

    /**
     * @param request
     * @param best
     */
    private void makeTransactionHappenBetweenClientAndProvider(
    		StringMessage request, IntegerMessage best) {
		// creating a contract group
		String contractGroupId = Instant.now().toString();
	
		// sending the location to the provider
		Message ack = sendMessageWithRoleAndWaitForReply(
			best.getSender(), // the address of the provider
			new StringMessage(contractGroupId), // send group's info
			MarketOrganization.BROKER_ROLE, // I am a broker
			1000); // I cannot wait the end of the universe
	
		if (ack != null) {
			// The provider has entered the contract group
			getLogger().info(() -> "Provider is ready !\nSending the contract" +
                    " number to client");
			// send group's info to the client
			sendReply(request, new StringMessage(contractGroupId));
			// let us celebrate and take vacation!!
			pause((int) (Math.random() * 2000 + 1000));
		}
		else { // no answer from the provider...
			getLogger().info(() -> "Provider disappears !!!!");
		}
    }

    /**
     * @param bids
     * @return
     */
    private IntegerMessage selectBestOffer(List<Message> bids) {
		final List<IntegerMessage> offers = Arrays.asList(
		        bids.toArray(new IntegerMessage[0]));// casting
		IntegerMessage best = ObjectMessage.min(offers);
		
		getLogger().info(() -> "The best offer is from " + best.getSender() +
				" " + best.getContent());
		return best;
    }

    @Override
    protected void end() {
        //launch another broker
        AgentAction.RELOAD.getActionFor(this).actionPerformed(null);
    }

    @Override
    public void setupFrame(AgentFrame frame) {
        JPanel p = new JPanel(new BorderLayout());
        // customizing but still using the OutputPanel from MaDKit GUI
        p.add(new OutputPanel(this), BorderLayout.CENTER);
        blinkPanel = new JPanel();
        blinkPanel.add(new JLabel(brokerImage));
        p.add(blinkPanel, BorderLayout.NORTH);
        blinkPanel.setBackground(Color.LIGHT_GRAY);
        p.validate();
        frame.add(p);
        int xLocation = nbOfBrokersOnScreen++ * 390;
        if (xLocation + 390 > Toolkit.getDefaultToolkit().getScreenSize().getWidth())
            nbOfBrokersOnScreen = 0;
        frame.setLocation(xLocation, 320);
        frame.setSize(390, 300);
    }

}
