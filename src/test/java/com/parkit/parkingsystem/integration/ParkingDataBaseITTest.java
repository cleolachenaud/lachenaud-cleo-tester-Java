package com.parkit.parkingsystem.integration;

import static org.mockito.Mockito.when;

import java.util.Date;

import org.apache.commons.lang.time.DateUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.parkit.parkingsystem.constants.Fare;
import com.parkit.parkingsystem.constants.ParkingType;
import com.parkit.parkingsystem.dao.ParkingSpotDAO;
import com.parkit.parkingsystem.dao.TicketDAO;
import com.parkit.parkingsystem.integration.config.DataBaseTestConfig;
import com.parkit.parkingsystem.integration.service.DataBasePrepareService;
import com.parkit.parkingsystem.model.Ticket;
import com.parkit.parkingsystem.service.ParkingService;
import com.parkit.parkingsystem.util.InputReaderUtil;

import junit.framework.Assert;

@ExtendWith(MockitoExtension.class)
public class ParkingDataBaseITTest {

	private static DataBaseTestConfig dataBaseTestConfig = new DataBaseTestConfig();
	private static ParkingSpotDAO parkingSpotDAO;
	private static TicketDAO ticketDAO;
	private static DataBasePrepareService dataBasePrepareService;

	@Mock
	private static InputReaderUtil inputReaderUtil;

	@BeforeAll
	private static void setUp() throws Exception{
		parkingSpotDAO = new ParkingSpotDAO();
		parkingSpotDAO.dataBaseConfig = dataBaseTestConfig;
		ticketDAO = new TicketDAO();
		ticketDAO.dataBaseConfig = dataBaseTestConfig;
		dataBasePrepareService = new DataBasePrepareService();
	}

	@BeforeEach
	private void setUpPerTest() throws Exception {
		when(inputReaderUtil.readSelection()).thenReturn(1);
		when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn("ABCDEF");
		dataBasePrepareService.clearDataBaseEntries();
	}

	@AfterAll
	private static void tearDown(){

	}
	/**
	 * permet de vérifier que la méthode processIncomingVehicle() insère bien en base de données
	 * le ticket correspondant à la plaque d'immatriculation.
	 */

	@Test
	public void parkingACarTest(){

		ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);

		// lancement metier
		parkingService.processIncomingVehicle();
		int prochainParkingSpot = parkingSpotDAO.getNextAvailableSlot(ParkingType.CAR);
		Ticket ticket = ticketDAO.getTicket("ABCDEF");
		Assert.assertEquals("l'heure de sortie ne doit pas être renseignée",null, ticket.getOutTime());
		Assert.assertEquals("la plaque d'immatriculation est ABCDEF", "ABCDEF", ticket.getVehicleRegNumber());
		Assert.assertEquals("le prix doit valoir 0.0", 0.0, ticket.getPrice());
		Assert.assertTrue("la place de parking doit être renseignée", ticket.getParkingSpot() != null);
		Assert.assertEquals("la prochaine place de parking doit être égale à 2", 2, prochainParkingSpot);
	}

	/**
	 * permet de vérifier que la méthode processExitingVehicle() insère bien en base de données
	 * les informations correspondantes au ticket correspondant.
	 */
	@Test
	public void parkingLotExitTest(){
		ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);
		parkingService.processIncomingVehicle();
		Ticket ticket = ticketDAO.getTicket("ABCDEF");
		Date outTime = DateUtils.addHours(ticketDAO.getTicket("ABCDEF").getInTime(), +2);
		parkingService.processExitingVehicle(outTime);

		ticket = ticketDAO.getTicket("ABCDEF");
		Assert.assertTrue("l'heure de sortie doit être renseignée", ticket.getOutTime() != null);
		Assert.assertTrue("la plaque d'immatriculation doit être renseignée", ticket.getVehicleRegNumber() != null);
		Assert.assertTrue("l'heure d'entrée doit être renseignée", ticket.getInTime() != null);

		int prochainParkingSpot = parkingSpotDAO.getNextAvailableSlot(ParkingType.CAR);
		Assert.assertEquals("la place de parking est libérée elle doit être égale à 1", 1, prochainParkingSpot);
	}

/**
 * Permet de vérifier que lors d'un utilisateur réccurent, la réduction des 5% s'applique à compter du 2e ticket.
 */
	@Test
	public void parkingLotExitRecurringUserTest () {
		ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);
		for (int passage = 0; passage < 2; passage ++) {
			parkingService.processIncomingVehicle();
			Date outTime = (Date) DateUtils.addHours(ticketDAO.getTicket("ABCDEF").getInTime(), +2 + passage);
			parkingService.processExitingVehicle(outTime);
		}
		Ticket ticket = ticketDAO.getTicket("ABCDEF");
		Assert.assertEquals("le numéro ID du ticket doit être égal à 2", 2, ticket.getId());
		Assert.assertEquals("la plaque doit être ABCDEF", "ABCDEF", ticket.getVehicleRegNumber());
		Assert.assertEquals("une réduction du prix doit être appliqué", (Fare.CAR_RATE_PER_HOUR * 0.95 * 3), ticket.getPrice());


	}

}
