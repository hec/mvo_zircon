package multivio.org.communication;

import java.io.IOException;
import java.io.*;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Servlet implementation
 */
public class Client extends HttpServlet {
	private static final long serialVersionUID = 1L;

	/**
	 * Default constructor.
	 */
	public Client() {

		super();
		// TODO Auto-generated constructor stub
	}

	/**
	 * @see Servlet#init(ServletConfig)
	 */
	public void init(ServletConfig config) throws ServletException {

		// TODO Auto-generated method stub
	}

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		//TO DO : modify this method to accept all query of the client
		String fileNumber = request.getParameter("req");
		System.out.println("value of the request " + fileNumber);
		ServerDocument serv = ServerDocument.getInstance();
		String res = serv.getMetadataDocument("http://doc.rero.ch/record/" + fileNumber + "/export/xd?");
		// do something
		response.setContentType("application/json");
		PrintWriter writer = response.getWriter();
		writer.print(res);
		writer.flush();
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
		String logwriter = request.getParameter("log");
		// TODO Auto-generated method stub
		if (logwriter != null) {			
			InputStream in = request.getInputStream();
			try {
				BufferedWriter out = new BufferedWriter(new FileWriter("../webapps/zircon/log.txt", true));
				byte[] buffer = new byte[2048];
				int read = in.read(buffer);
				String st = new String();
				while (read != -1) {
					st = new String(buffer, 0, read);
					out.write(st);
					read = in.read(buffer);
					out.flush();
				}
				out.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			in.close();
		}	
	}

}
