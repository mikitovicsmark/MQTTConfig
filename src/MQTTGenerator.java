import java.util.Iterator;

import Allocation.CPS;
import Allocation.Hardware;

public class MQTTGenerator {

        public static void main(String[] args) {

                EMFModelLoad loader = new EMFModelLoad();
                CPS cps = loader.load();

                System.out.println(cps);

                for (Iterator<Hardware> iterator = cps.getHardwares().iterator(); iterator
                                .hasNext();) {
                        Hardware hw = iterator.next();
                }
        }
}
