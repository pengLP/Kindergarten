package com.kindergarten.controller;

import com.kindergarten.po.Kindergarten;
import com.kindergarten.po.SuccessUser;
import com.kindergarten.po.User;
import com.kindergarten.service.KindergartenService;
import com.kindergarten.service.SuccessUserService;
import com.kindergarten.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.ClassUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

@Controller
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private KindergartenService kindergartenService;

    @Autowired
    private SuccessUserService successUserService;

    @RequestMapping("/login")
    public String index(Kindergarten kindergarten , Model model , HttpServletRequest request) {
        Kindergarten k = kindergartenService.getKindergartenById(kindergarten);
        request.getSession().setAttribute("kindergarten", k);
        return "login";
    }

    @RequestMapping("index")
    public String toIndex(HttpServletRequest request , Model model){
        User u = (User) request.getSession().getAttribute("user");
        if (u.getStatus().equals("未报名"))
            return "index";
        else{
            model.addAttribute("msg", u.getStatus());
            return "result";
        }
    }

    @RequestMapping("/toRegister")
    public String toRegister(Kindergarten kindergarten) {
        return "register";
    }

    public static boolean isEffectiveDate(Date nowTime, Date startTime, Date endTime) {
        if (nowTime.getTime() != startTime.getTime() && nowTime.getTime() != endTime.getTime()) {
            Calendar date = Calendar.getInstance();
            date.setTime(nowTime);
            Calendar begin = Calendar.getInstance();
            begin.setTime(startTime);
            Calendar end = Calendar.getInstance();
            end.setTime(endTime);
            return date.after(begin) && date.before(end);
        } else {
            return true;
        }
    }

    @RequestMapping("/register")
    public String register(@RequestParam("files") MultipartFile[] files, User user, HttpServletRequest request, Model model) {
        Kindergarten kindergarten = (Kindergarten) request.getSession().getAttribute("kindergarten");
        String year = user.getCardNum().substring(6, 10);
        String month = user.getCardNum().substring(10, 12);
        String day = user.getCardNum().substring(12, 14);
        String birthday = year + "-" + month + "-" + day;
        DateFormat fmt = new SimpleDateFormat("yyyy-MM-dd");
        Date date = null;

        try {
            date = fmt.parse(birthday);
            System.out.println(date);
            if (isEffectiveDate(date, fmt.parse("2015-09-01"), fmt.parse("2016-08-31"))) {
                user.setType("小班");
            } else {
                if (!isEffectiveDate(date, fmt.parse("2016-09-01"), fmt.parse("2017-02-28"))) {
                    user.setType("拒收");
                    model.addAttribute("msg", "对不起，年龄不到无法报名。感谢您对"+kindergarten.getName()+"的支持！相关详细信息，请关注我园公众微信平台！");
                    return "error";
                }
                user.setType("托班");
            }
        } catch (ParseException var15) {
            var15.printStackTrace();
        }


        List<String> names = new ArrayList();
        String p = "";
        String realPath;
        String fileName;
        if (files != null && files.length > 0) {
            for(int i = 0; i < files.length; ++i) {
                MultipartFile file = files[i];
                realPath =  File.separator+"home"+File.separator+"ubuntu";
                fileName = user.getkId()+File.separator+user.getCardNum()  + File.separator+ System.currentTimeMillis() + file.getOriginalFilename();
                String filePath = realPath + File.separator + "img"+File.separator +  fileName;
                p = filePath;
                names.add(fileName);
                System.out.println(filePath);
                try {
                    File f = new File(filePath);
                    if (!f.getParentFile().exists())
                        f.getParentFile().mkdirs();
                    file.transferTo(new File(filePath));
                } catch (IOException var16) {
                    var16.printStackTrace();
                }
            }
        }

        user.setStatus("未报名");
        user.setPassword(user.getCardNum().substring(user.getCardNum().length() - 6, user.getCardNum().length()));
        user.setCardZPic((String)names.get(0));
        user.setCardFPic((String)names.get(1));
        user.setHksyPic((String)names.get(2));
        user.setHkhzPic((String)names.get(3));
        user.setHketbryPic((String)names.get(4));
        user.setEtyfjzz((String)names.get(5));
        user.setFczPic((String)names.get(6));
        user.setRegtime(new Date());
        model.addAttribute("path", p);
        userService.addUser(user);
        return "login";
    }

    @RequestMapping({"/checkLogin"})
    public String checkLogin(User user, Model model, HttpServletRequest request) {
        List<User> users = userService.getUser(user);
        System.out.println(user);
        if (users.size() > 0) {
            model.addAttribute("user", users.get(0));
            request.getSession().setAttribute("user", users.get(0));
            User u = users.get(0);
            if (u.getStatus().equals("未报名"))
                return "index";
            else{
                model.addAttribute("msg", u.getStatus());
                return "result";
            }
        } else {
            model.addAttribute("msg", "用户名或密码错误");
            return "error";
        }
    }


    @RequestMapping("/getUserInfo")
    public String getUserInfo() {
        return "update";
    }

    @RequestMapping("/updatePass")
    public String updatePass() {
        return "updatePass";
    }

    @RequestMapping("/updatePassword")
    public String updatePassword(User user , String password2 , HttpServletRequest request,Model model) {
        User u = (User) request.getSession().getAttribute("user");
        if (u.getPassword().equals(user.getPassword())) {
            u.setPassword(password2);
            userService.updateUser(u);
            return "index";
        }
        model.addAttribute("msg","原密码错误");
        return "error";
    }

    @RequestMapping("bm")
    public String bm(HttpServletRequest request,Model model) {
        User u = (User) request.getSession().getAttribute("user");
        Kindergarten kindergarten = (Kindergarten) request.getSession().getAttribute("kindergarten");
        String type = u.getType();
        int num = 0;
        if (type.equals("小班"))
            num = kindergarten.getXbNum();
        else
            num = kindergarten.getDbNum();
        SuccessUser successUser = new SuccessUser();
        successUser.setType(type);
        successUser.setUserId(u.getId());
        successUser.setkId(kindergarten.getId());
        List<SuccessUser> successUsers = successUserService.getSuccessUser(successUser);
        if (successUsers.size() > num) {
            u.setStatus("很遗憾，截止到您提交报名之前，"+kindergarten.getName()+"幼儿园名额已满");
            userService.updateUser(u);
            model.addAttribute("msg", "很遗憾，截止到您提交报名之前，"+kindergarten.getName()+"幼儿园名额已满");
            return "result";
        }else {

            successUserService.addSuccessUser(successUser);
            u.setStatus("恭喜您，报名成功，请于"+kindergarten.getBdTime()+"来"+kindergarten.getName()+"幼儿园报到");
            u.setBmtime(new Date());
            userService.updateUser(u);
            model.addAttribute("msg" , "恭喜您，报名成功，请于"+kindergarten.getDbNum()+"来"+kindergarten.getName()+"幼儿园报到");
            return "result";
        }
    }

}