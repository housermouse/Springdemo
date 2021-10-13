//package com.apexsoft;
//
//import com.apexsoft.framework.rbac.base.common.dto.AccountDto;
//import com.apexsoft.framework.rbac.base.common.dto.LoginDto;
//import com.apexsoft.framework.rbac.base.common.dto.RightsDto;
//import com.apexsoft.framework.rbac.base.user.api.IUserService;
//import com.apexsoft.framework.rbac.base.user.model.User;
//import com.apexsoft.live.exception.AuthException;
//import com.apexsoft.live.session.AbstractUserAuthenticate;
//import com.apexsoft.live.session.UserAuthenticateContext;
//import com.google.gson.Gson;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.context.annotation.Primary;
//import org.springframework.stereotype.Service;
//import org.springframework.util.Assert;
//
//import javax.servlet.http.HttpServletRequest;
//import javax.servlet.http.HttpServletResponse;
//import java.io.BufferedReader;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//
///**
// * @Description: ${todo}
// * @author: Logan
// * @date: 2020-12-23 16:53
// */
//@Service
//@Primary
//public class DemoDefaultAuthService extends AbstractUserAuthenticate {
//    private static Logger logger = LoggerFactory.getLogger(DemoDefaultAuthService.class);
//    @Autowired
//    private IUserService userService;
//
//    @Override
//    public void auth(UserAuthenticateContext context, HttpServletRequest request, HttpServletResponse response) throws AuthException{
//        try {
//            BufferedReader reader = request.getReader();
//            StringBuilder builder = new StringBuilder();
//            String line = reader.readLine();
//            while (line != null) {
//                builder.append(line);
//                line = reader.readLine();
//            }
//            reader.close();
//            String reqBody = builder.toString();
//            Gson gson = new Gson();
//            LoginDto loginDto = gson.fromJson(reqBody, LoginDto.class);
//            User user = userService.getByLoginId(loginDto.getLoginId());
//            Assert.notNull(loginDto.getLoginId(), "登陆账号不存在或者密码不正确");
//
//            RightsDto menuRights = userService.getRights(loginDto.getLoginId());
//            if (user.getAdmin()) {
//                menuRights.setElementEnabled(false);
//            }
//            AccountDto dto = new AccountDto(user);
//            dto.setAuth(menuRights);
//            context.setUserinfo(dto);
//
//            Map<String, Object> data = new HashMap<>();
//            data.put("code", 1);
//            data.put("note", "登录成功");
//            data.put("user", dto);
//            context.setAuthResponse(data);
//
//        } catch (Exception e) {
//            logger.error("登陆异常",e);
//            throw new AuthException("登陆异常");
//        }
//
//
//    }
//
//    @Override
//    public List<String> getExcludeUrls() {
//        List<String> excludeUrls = super.getExcludeUrls();
//        excludeUrls.add("/");
//        excludeUrls.add("/_data/auth/login");
//        excludeUrls.add("/_data/auth/logout");
//        excludeUrls.add("/public/*");
//
//        return excludeUrls;
//    }
//
//
//
//
//
//    @Override
//    public String getLoginUrl() {
//        return "/_data/auth/login";
//    }
//
//    //    @Override
////    public AuthResponse auth(AuthData authData, HttpServletRequest request, HttpServletResponse response) throws AuthException {
////        String loginId = authData.getUser();
////        RightsDto menuRights = userService.getRights(loginId);
////        User user = userService.getByLoginId(loginId);
////        if(user.getAdmin()) {
////            menuRights.setElementEnabled(false);
////        }
////        AccountDto dto = new AccountDto(user);
////        dto.setAuth(menuRights);
////        AuthUser<AccountDto> authUser = new AuthUser<>();
////        authUser.setUserId(dto.getLoginId());
////        authUser.setUserName(dto.getLoginName());
////        authUser.setUser(dto);
////        return new AuthResponse(JSONResponse.CODE_SUCCESS, "", authUser);
////
////    }
//}
