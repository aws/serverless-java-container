package ${groupId}.actions;


import com.opensymphony.xwork2.ModelDriven;
import org.apache.struts2.rest.DefaultHttpHeaders;
import org.apache.struts2.rest.HttpHeaders;
import org.apache.struts2.rest.RestActionSupport;


import java.util.Collection;
import java.util.UUID;


public class PingController extends RestActionSupport implements ModelDriven<Object> {

    private String model = new String();
    private String id;
    private Collection<String> list = null;


    // GET /ping/1
    public HttpHeaders show() {
        return new DefaultHttpHeaders("show");
    }

    // GET /ping
    public HttpHeaders index() {
        this.model = "Hello, World!";
        return new DefaultHttpHeaders("index")
                .disableCaching();
    }

    // POST /ping
    public HttpHeaders create() {
        this.model = UUID.randomUUID().toString();
        return new DefaultHttpHeaders("success")
                .setLocationId(model);

    }

    // PUT /ping/1
    public String update() {
        //TODO: UPDATE LOGIC
        return SUCCESS;
    }

    // DELETE /ping/1
    public String destroy() {
        //TODO: DELETE LOGIC
        return SUCCESS;
    }

    public void setId(String id) {
        if (id != null) {
            this.model = "New model instance";
        }
        this.id = id;
    }

    public Object getModel() {
        if (list != null) {
            return list;
        } else {
            if (model == null) {
                model = "Pong";
            }
            return model;
        }
    }
}
