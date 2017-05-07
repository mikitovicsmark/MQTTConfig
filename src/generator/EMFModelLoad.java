package generator;
import java.util.Map;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;

import Allocation.CPS;
import Allocation.AllocationPackage;

public class EMFModelLoad {
        public CPS load() {
                // Initialize the model
                AllocationPackage.eINSTANCE.eClass();

                Resource.Factory.Registry reg = Resource.Factory.Registry.INSTANCE;
                Map<String, Object> m = reg.getExtensionToFactoryMap();
                m.put("xmi", new XMIResourceFactoryImpl());

                // Obtain a new resource set
                ResourceSet resSet = new ResourceSetImpl();

                // Get the resource
                Resource resource = resSet.getResource(URI
                                .createURI("model/CPS2.xmi"), true);
                // Get the first model element and cast it to the right type, in my
                // example everything is hierarchical included in this first node
                CPS cps = (CPS) resource.getContents().get(0);
                return cps;
        }

}
