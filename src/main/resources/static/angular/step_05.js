var phonecatApp = angular.module('phonecatApp', []);

phonecatApp.controller('PhoneListCtrl', function ($scope, $http) {
  $http.get('/phonecat/phones').success(function(data) {
      $scope.phones = data;
      //$scope.phones = data.splice(0, 5);
  });

  $scope.orderProp = 'age';
});