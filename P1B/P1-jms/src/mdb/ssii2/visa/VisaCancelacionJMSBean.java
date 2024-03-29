/**
 * Pr&aacute;ctricas de Sistemas Inform&aacute;ticos II
 * VisaCancelacionJMSBean.java
 */

package ssii2.visa;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.ejb.EJBException;
import javax.ejb.MessageDriven;
import javax.ejb.MessageDrivenContext;
import javax.ejb.ActivationConfigProperty;
import javax.jms.MessageListener;
import javax.jms.Message;
import javax.jms.TextMessage;
import javax.jms.JMSException;
import javax.annotation.Resource;
import java.util.logging.Logger;

/**
 * @author jaime
 */
@MessageDriven(mappedName = "jms/VisaPagosQueue")
public class VisaCancelacionJMSBean extends DBTester implements MessageListener {
  static final Logger logger = Logger.getLogger("VisaCancelacionJMSBean");
  @Resource
  private MessageDrivenContext mdc;

  private static final String UPDATE_CANCELA_QRY =
                "update pago " +
                "set codrespuesta=999 " +
                "where idautorizacion=? ";

  private static final String FIX_SALDO_QRY =
                "update tarjeta " +
                "set saldo=pago.importe + saldo " +
                "from pago " +
                "where pago.idautorizacion=? and pago.numerotarjeta = tarjeta.numerotarjeta ";


  public VisaCancelacionJMSBean() {
  }

  // TODO : Método onMessage de ejemplo
  // Modificarlo para ejecutar el UPDATE definido más arriba,
  // asignando el idAutorizacion a lo recibido por el mensaje
  // Para ello conecte a la BD, prepareStatement() y ejecute correctamente
  // la actualización
  public void onMessage(Message inMessage) {
      TextMessage msg = null;
      PreparedStatement pstmt = null;
      Connection con = null;

      try {
          if (inMessage instanceof TextMessage) {
              msg = (TextMessage) inMessage;
              logger.info("MESSAGE BEAN: Message received: " + msg.getText());

              // Obtener conexion
              con = getConnection();

              // Asignamos el código de respuesta a 999 del idautorizacion recibido en el mensaje
              int idAutorizacion = Integer.parseInt(msg.getText());

              String cancela = UPDATE_CANCELA_QRY;
              pstmt = con.prepareStatement(cancela);
              pstmt.setInt(1, idAutorizacion);

              if (pstmt.execute()
                    || pstmt.getUpdateCount() != 1) {
                logger.warning("ERROR al actualizar el codigo de respuesta a valor 999.\n");
              }
              pstmt.close();
              pstmt = null;

              // Rectificamos el saldo de la tarjeta que realizó el pago
              String fixSaldo = FIX_SALDO_QRY;
              pstmt = con.prepareStatement(fixSaldo);
              pstmt.setInt(1, idAutorizacion);

              if (pstmt.execute()
                    || pstmt.getUpdateCount() != 1) {
                logger.warning("ERROR al rectificar el saldo de la tarjeta que realizo el pago.\n");
              }
              pstmt.close();
              pstmt = null;

          } else {
              logger.warning(
                      "Message of wrong type: "
                      + inMessage.getClass().getName());
          }
      } catch (JMSException e) {
          e.printStackTrace();
          mdc.setRollbackOnly();
      } catch (Throwable te) {
          te.printStackTrace();
      }
  }


}
