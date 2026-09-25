package main
import (
 "encoding/json"
 "net/http"
 "testing"
 "time"
 "github.com/golang-jwt/jwt/v5"
 "github.com/samber/mo"
 "github.com/stretchr/testify/require"
)
func TestDuskRainStaleValidateRefresh(t *testing.T) {
 ts:=&TestSuite{};ts.Setup(testConfig());defer ts.Teardown()
 ts.CreateTestUser(t,ts.App,ts.Server,TEST_USERNAME)
 a:=ts.authenticate(t,TEST_PLAYER_NAME,TEST_PASSWORD)
 client,err:=ts.App.GetClient(a.AccessToken,mo.Some(a.ClientToken),StalePolicyDeny,false);require.NoError(t,err)
 claims:=TokenClaims{RegisteredClaims:jwt.RegisteredClaims{Subject:client.UUID,Issuer:"drasl",ExpiresAt:jwt.NewNumericDate(time.Now().Add(time.Hour)),IssuedAt:jwt.NewNumericDate(time.Now().Add(-time.Hour))},Version:client.Version,StaleAt:jwt.NewNumericDate(time.Now().Add(-time.Minute))}
 stale,err:=jwt.NewWithClaims(jwt.SigningMethodRS512,claims).SignedString(ts.App.PrivateKey);require.NoError(t,err)
 v:=ts.PostJSON(t,ts.Server,"/validate",validateRequest{AccessToken:stale,ClientToken:a.ClientToken},nil,nil);require.Equal(t,http.StatusForbidden,v.Code)
 f:=ts.PostJSON(t,ts.Server,"/refresh",refreshRequest{AccessToken:stale,ClientToken:a.ClientToken},nil,nil);require.Equal(t,http.StatusOK,f.Code)
 var refreshed refreshResponse;require.NoError(t,json.NewDecoder(f.Body).Decode(&refreshed))
 ok:=ts.PostJSON(t,ts.Server,"/validate",validateRequest{AccessToken:refreshed.AccessToken,ClientToken:a.ClientToken},nil,nil);require.Equal(t,http.StatusNoContent,ok.Code)
 _,err=ts.App.GetClient(refreshed.AccessToken,mo.Some(a.ClientToken),StalePolicyDeny,false);require.NoError(t,err)
 old:=ts.PostJSON(t,ts.Server,"/validate",validateRequest{AccessToken:stale,ClientToken:a.ClientToken},nil,nil);require.Equal(t,http.StatusForbidden,old.Code)
}
