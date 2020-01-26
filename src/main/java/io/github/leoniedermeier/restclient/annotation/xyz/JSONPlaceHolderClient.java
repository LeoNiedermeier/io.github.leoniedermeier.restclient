package io.github.leoniedermeier.restclient.annotation.xyz;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import io.github.leoniedermeier.restclient.annotation.RestClient;

@RestClient(value = "jplaceholder", url = "https://jsonplaceholder.typicode.com/")
public interface JSONPlaceHolderClient {
    
    // produces / consumes durch messageConverter bestimmt
    // org.springframework.web.client.RestTemplate.AcceptHeaderRequestCallback.doWithRequest(ClientHttpRequest)
    @RequestMapping(method = RequestMethod.GET, value = "/posts/{postId}")
    Post getPostById(@PathVariable("postId") Long postIdPArameter);

    @RequestMapping(method = RequestMethod.GET, value = "/posts", consumes = "application/json")
    List<Post> getPosts();
    
  //  https://jsonplaceholder.typicode.com/posts?userId=1
    @GetMapping(  value = "/posts")
    List<Post> getPostsByUserId(@RequestParam String userId);
}